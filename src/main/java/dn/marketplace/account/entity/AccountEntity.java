package dn.marketplace.account.entity;

import dn.marketplace.account.api.enums.BusinessStatus;
import dn.marketplace.core.exception.BusinessRuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Проекция аккаунта маркетплейса. Id — {@code sub} из Keycloak.
 * Переходы статусов — методы этой сущности; профиль обновляет только {@link #applyProfile}. Сеттеров нет.
 */
@Entity
@Table(schema = "market_place", name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntity implements Persistable<UUID> {

    public static final int MAX_SELLER_APPLICATIONS = 5;

    @Id
    private UUID id;

    @Column(name = "user_name", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_status", nullable = false)
    private BusinessStatus businessStatus = BusinessStatus.BUYER;

    @Column(nullable = false)
    private boolean banned;

    @Column(name = "email_snapshot")
    private String emailSnapshot;

    @Column(name = "first_name_snapshot")
    private String firstNameSnapshot;

    @Column(name = "last_name_snapshot")
    private String lastNameSnapshot;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // SMALLINT в DDL: без JdbcTypeCode Hibernate 7 ждёт INTEGER и падает на ddl-auto=validate
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "seller_applications", nullable = false)
    private int sellerApplications;

    @Column(name = "seller_application_hold_until")
    private Instant sellerApplicationHoldUntil;

    @Version
    private Long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static AccountEntity create(UUID id, String username) {
        AccountEntity entity = new AccountEntity();
        entity.id = id;
        entity.username = username;
        entity.businessStatus = BusinessStatus.BUYER;
        return entity;
    }

    @Override
    public boolean isNew() {
        return version == null;
    }

    public void applyAsSeller(Instant now, Duration hold) {
        assertNotDeleted();
        if (banned) {
            throw new BusinessRuleViolationException("Забаненный аккаунт не может подать заявку на продавца");
        }
        if (sellerApplicationHoldUntil != null && now.isBefore(sellerApplicationHoldUntil)) {
            throw new BusinessRuleViolationException(
                    "Повторная заявка возможна после " + sellerApplicationHoldUntil);
        }
        if (businessStatus != BusinessStatus.BUYER && businessStatus != BusinessStatus.SELLER_REJECTED) {
            throw new BusinessRuleViolationException("Заявку можно подать только из статуса покупателя или после отказа");
        }
        // Сброс истёкшего холда — только после всех предусловий: при 422 сущность остаётся нетронутой
        if (sellerApplicationHoldUntil != null) {
            sellerApplications = 0;
            sellerApplicationHoldUntil = null;
        }
        rejectionReason = null;
        sellerApplications++;
        businessStatus = BusinessStatus.SELLER_PENDING;
        if (sellerApplications == MAX_SELLER_APPLICATIONS) {
            sellerApplicationHoldUntil = now.plus(hold);
        }
    }

    public void approveSeller() {
        assertNotDeleted();
        assertStatus(BusinessStatus.SELLER_PENDING, "одобрить заявку");
        businessStatus = BusinessStatus.SELLER;
        sellerApplications = 0;
        sellerApplicationHoldUntil = null;
        rejectionReason = null;
    }

    public void rejectSeller(String reason) {
        assertNotDeleted();
        assertStatus(BusinessStatus.SELLER_PENDING, "отклонить заявку");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleViolationException("Причина отказа обязательна");
        }
        businessStatus = BusinessStatus.SELLER_REJECTED;
        rejectionReason = reason;
    }

    public void revokeSeller() {
        assertNotDeleted();
        assertStatus(BusinessStatus.SELLER, "отозвать статус продавца");
        businessStatus = BusinessStatus.BUYER;
    }

    public void ban() {
        assertNotDeleted();
        if (banned) {
            throw new BusinessRuleViolationException("Аккаунт уже забанен");
        }
        banned = true;
    }

    public void unban() {
        assertNotDeleted();
        if (!banned) {
            throw new BusinessRuleViolationException("Аккаунт не забанен");
        }
        banned = false;
    }

    public void delete(Instant now) {
        assertNotDeleted();
        deletedAt = now;
        emailSnapshot = null;
        firstNameSnapshot = null;
        lastNameSnapshot = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void applyProfile(String username, String email, String firstName, String lastName) {
        if (isDeleted()) {
            return;
        }
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
        this.emailSnapshot = email;
        this.firstNameSnapshot = firstName;
        this.lastNameSnapshot = lastName;
    }

    private void assertNotDeleted() {
        if (isDeleted()) {
            throw new BusinessRuleViolationException("Аккаунт удалён");
        }
    }

    private void assertStatus(BusinessStatus expected, String action) {
        if (businessStatus != expected) {
            throw new BusinessRuleViolationException("Нельзя " + action + " из статуса " + businessStatus);
        }
    }
}
