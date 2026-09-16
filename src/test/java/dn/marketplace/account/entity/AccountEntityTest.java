package dn.marketplace.account.entity;

import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.core.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountEntityTest {

    private static final Duration HOLD = Duration.ofMinutes(3);
    private static final Instant T0 = Instant.parse("2026-09-16T10:00:00Z");

    private AccountEntity buyer() {
        return AccountEntity.create(UUID.randomUUID(), "alice");
    }

    @Test
    void applyAsSeller_из_покупателя_в_ожидание() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER_PENDING);
        assertThat(account.getSellerApplications()).isEqualTo(1);
        assertThat(account.getRejectionReason()).isNull();
    }

    @Test
    void approveSeller_делает_продавцом_и_обнуляет_счётчик() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        account.approveSeller();
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER);
        assertThat(account.getSellerApplications()).isZero();
        assertThat(account.getSellerApplicationHoldUntil()).isNull();
    }

    @Test
    void rejectSeller_пишет_причину() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        account.rejectSeller("документы");
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER_REJECTED);
        assertThat(account.getRejectionReason()).isEqualTo("документы");
    }

    @Test
    void rejectSeller_без_причины_запрещён() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        assertThatThrownBy(() -> account.rejectSeller("  "))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void revokeSeller_возвращает_в_покупатели() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        account.approveSeller();
        account.revokeSeller();
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.BUYER);
    }

    @Test
    void ban_не_меняет_статус() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        account.approveSeller();
        account.ban();
        assertThat(account.isBanned()).isTrue();
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER);
    }

    @Test
    void забаненный_не_может_подать_заявку() {
        AccountEntity account = buyer();
        account.ban();
        assertThatThrownBy(() -> account.applyAsSeller(T0, HOLD))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Забаненный");
    }

    @Test
    void админ_может_одобрить_забаненного() {
        AccountEntity account = buyer();
        account.applyAsSeller(T0, HOLD);
        account.ban();
        account.approveSeller();
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER);
        assertThat(account.isBanned()).isTrue();
    }

    @Test
    void лимит_заявок_шестая_после_отказа_в_холде() {
        AccountEntity account = buyer();
        for (int i = 0; i < AccountEntity.MAX_SELLER_APPLICATIONS; i++) {
            account.applyAsSeller(T0, HOLD);
            if (i < AccountEntity.MAX_SELLER_APPLICATIONS - 1) {
                account.rejectSeller("нет");
            }
        }
        assertThat(account.getSellerApplications()).isEqualTo(5);
        assertThat(account.getSellerApplicationHoldUntil()).isEqualTo(T0.plus(HOLD));
        account.rejectSeller("нет");
        assertThatThrownBy(() -> account.applyAsSeller(T0.plusSeconds(10), HOLD))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Повторная заявка");
    }

    @Test
    void после_холда_счётчик_сбрасывается() {
        AccountEntity account = buyer();
        for (int i = 0; i < AccountEntity.MAX_SELLER_APPLICATIONS; i++) {
            account.applyAsSeller(T0, HOLD);
            account.rejectSeller("нет");
        }
        account.applyAsSeller(T0.plus(HOLD), HOLD);
        assertThat(account.getSellerApplications()).isEqualTo(1);
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.SELLER_PENDING);
    }

    @Test
    void delete_обнуляет_снапшоты() {
        AccountEntity account = buyer();
        account.applyProfile("alice", "a@b.c", "A", "B");
        account.delete(T0);
        assertThat(account.isDeleted()).isTrue();
        assertThat(account.getEmailSnapshot()).isNull();
        assertThat(account.getFirstNameSnapshot()).isNull();
        assertThat(account.getLastNameSnapshot()).isNull();
    }

    @Test
    void переход_на_удалённом_запрещён() {
        AccountEntity account = buyer();
        account.delete(T0);
        assertThatThrownBy(() -> account.applyAsSeller(T0, HOLD))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("удалён");
        assertThatThrownBy(account::ban)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "approveSeller",
            "revokeSeller"
    })
    void запрещённые_переходы_из_покупателя(String method) {
        AccountEntity account = buyer();
        assertThatThrownBy(() -> invoke(account, method))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    private static void invoke(AccountEntity account, String method) {
        switch (method) {
            case "approveSeller" -> account.approveSeller();
            case "revokeSeller" -> account.revokeSeller();
            default -> throw new IllegalArgumentException(method);
        }
    }
}
