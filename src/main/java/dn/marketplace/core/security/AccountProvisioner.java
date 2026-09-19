package dn.marketplace.core.security;

import java.util.UUID;

/**
 * Порт, через который инфраструктура безопасности просит домен {@code account}
 * гарантировать наличие строки аккаунта.
 * <p>
 * Интерфейс живёт в {@code core}: домен реализует порт, ядро не знает о домене.
 * Реализация — {@code dn.marketplace.account.AccountFacade}.
 */
@FunctionalInterface
public interface AccountProvisioner {

    /**
     * Идемпотентно создаёт аккаунт, если его ещё нет.
     * Ровно один {@code INSERT ... ON CONFLICT DO NOTHING}.
     *
     * @param accountId значение claim {@code sub} из JWT Keycloak
     * @param username  {@code preferred_username}; колонка {@code user_name} NOT NULL
     */
    void provision(UUID accountId, String username);
}
