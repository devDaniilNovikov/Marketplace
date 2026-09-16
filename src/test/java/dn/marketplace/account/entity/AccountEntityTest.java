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

    @Test
    void истёкший_холд_не_сбрасывается_если_переход_запрещён() {
        AccountEntity account = buyer();
        for (int i = 0; i < AccountEntity.MAX_SELLER_APPLICATIONS; i++) {
            account.applyAsSeller(T0, HOLD);
            if (i < AccountEntity.MAX_SELLER_APPLICATIONS - 1) {
                account.rejectSeller("нет");
            }
        }
        // 5-я заявка в SELLER_PENDING, холд выставлен и уже истёк — но из PENDING подать нельзя
        assertThatThrownBy(() -> account.applyAsSeller(T0.plus(HOLD), HOLD))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(account.getSellerApplications())
                .as("при 422 сущность не должна остаться полуизменённой")
                .isEqualTo(5);
        assertThat(account.getSellerApplicationHoldUntil()).isEqualTo(T0.plus(HOLD));
    }

    /**
     * Полный граф переходов из раздела 3 спеки: 4 статуса × banned × 7 методов.
     * Ожидание — статус после перехода либо 422 (BusinessRuleViolationException).
     */
    @ParameterizedTest(name = "{0} banned={1} → {2} ⇒ {3}")
    @CsvSource({
            // from,            banned, method,        expected
            "BUYER,             false,  applyAsSeller, SELLER_PENDING",
            "BUYER,             false,  approveSeller, 422",
            "BUYER,             false,  rejectSeller,  422",
            "BUYER,             false,  revokeSeller,  422",
            "BUYER,             false,  ban,           BUYER",
            "BUYER,             false,  unban,         422",
            "BUYER,             false,  delete,        BUYER",
            "BUYER,             true,   applyAsSeller, 422",
            "BUYER,             true,   approveSeller, 422",
            "BUYER,             true,   rejectSeller,  422",
            "BUYER,             true,   revokeSeller,  422",
            "BUYER,             true,   ban,           422",
            "BUYER,             true,   unban,         BUYER",
            "BUYER,             true,   delete,        BUYER",
            "SELLER_PENDING,    false,  applyAsSeller, 422",
            "SELLER_PENDING,    false,  approveSeller, SELLER",
            "SELLER_PENDING,    false,  rejectSeller,  SELLER_REJECTED",
            "SELLER_PENDING,    false,  revokeSeller,  422",
            "SELLER_PENDING,    false,  ban,           SELLER_PENDING",
            "SELLER_PENDING,    false,  unban,         422",
            "SELLER_PENDING,    false,  delete,        SELLER_PENDING",
            "SELLER_PENDING,    true,   applyAsSeller, 422",
            "SELLER_PENDING,    true,   approveSeller, SELLER",
            "SELLER_PENDING,    true,   rejectSeller,  SELLER_REJECTED",
            "SELLER_PENDING,    true,   revokeSeller,  422",
            "SELLER_PENDING,    true,   ban,           422",
            "SELLER_PENDING,    true,   unban,         SELLER_PENDING",
            "SELLER_PENDING,    true,   delete,        SELLER_PENDING",
            "SELLER,            false,  applyAsSeller, 422",
            "SELLER,            false,  approveSeller, 422",
            "SELLER,            false,  rejectSeller,  422",
            "SELLER,            false,  revokeSeller,  BUYER",
            "SELLER,            false,  ban,           SELLER",
            "SELLER,            false,  unban,         422",
            "SELLER,            false,  delete,        SELLER",
            "SELLER,            true,   applyAsSeller, 422",
            "SELLER,            true,   approveSeller, 422",
            "SELLER,            true,   rejectSeller,  422",
            "SELLER,            true,   revokeSeller,  BUYER",
            "SELLER,            true,   ban,           422",
            "SELLER,            true,   unban,         SELLER",
            "SELLER,            true,   delete,        SELLER",
            "SELLER_REJECTED,   false,  applyAsSeller, SELLER_PENDING",
            "SELLER_REJECTED,   false,  approveSeller, 422",
            "SELLER_REJECTED,   false,  rejectSeller,  422",
            "SELLER_REJECTED,   false,  revokeSeller,  422",
            "SELLER_REJECTED,   false,  ban,           SELLER_REJECTED",
            "SELLER_REJECTED,   false,  unban,         422",
            "SELLER_REJECTED,   false,  delete,        SELLER_REJECTED",
            "SELLER_REJECTED,   true,   applyAsSeller, 422",
            "SELLER_REJECTED,   true,   approveSeller, 422",
            "SELLER_REJECTED,   true,   rejectSeller,  422",
            "SELLER_REJECTED,   true,   revokeSeller,  422",
            "SELLER_REJECTED,   true,   ban,           422",
            "SELLER_REJECTED,   true,   unban,         SELLER_REJECTED",
            "SELLER_REJECTED,   true,   delete,        SELLER_REJECTED",
    })
    void таблица_переходов(BusinessStatus from, boolean banned, String method, String expected) {
        AccountEntity account = inStatus(from, banned);

        if ("422".equals(expected)) {
            assertThatThrownBy(() -> invoke(account, method))
                    .isInstanceOf(BusinessRuleViolationException.class);
            assertThat(account.getBusinessStatus()).as("статус при 422 не меняется").isEqualTo(from);
            assertThat(account.isBanned()).as("бан при 422 не меняется").isEqualTo(banned);
            return;
        }

        invoke(account, method);
        assertThat(account.getBusinessStatus()).isEqualTo(BusinessStatus.valueOf(expected));
        switch (method) {
            case "ban" -> assertThat(account.isBanned()).isTrue();
            case "unban" -> assertThat(account.isBanned()).isFalse();
            case "delete" -> {
                assertThat(account.isDeleted()).isTrue();
                assertThat(account.isBanned()).as("delete не трогает бан").isEqualTo(banned);
            }
            default -> assertThat(account.isBanned()).as("переход статуса не трогает бан").isEqualTo(banned);
        }
    }

    private AccountEntity inStatus(BusinessStatus status, boolean banned) {
        AccountEntity account = buyer();
        switch (status) {
            case BUYER -> { }
            case SELLER_PENDING -> account.applyAsSeller(T0, HOLD);
            case SELLER -> {
                account.applyAsSeller(T0, HOLD);
                account.approveSeller();
            }
            case SELLER_REJECTED -> {
                account.applyAsSeller(T0, HOLD);
                account.rejectSeller("нет");
            }
        }
        if (banned) {
            account.ban();
        }
        return account;
    }

    private static void invoke(AccountEntity account, String method) {
        switch (method) {
            case "applyAsSeller" -> account.applyAsSeller(T0, HOLD);
            case "approveSeller" -> account.approveSeller();
            case "rejectSeller" -> account.rejectSeller("причина");
            case "revokeSeller" -> account.revokeSeller();
            case "ban" -> account.ban();
            case "unban" -> account.unban();
            case "delete" -> account.delete(T0);
            default -> throw new IllegalArgumentException(method);
        }
    }
}
