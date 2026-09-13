package dn.marketplace.core.security;

import java.util.UUID;

/**
 * Порт, через который инфраструктура безопасности просит домен {@code account}
 * гарантировать наличие строки аккаунта.
 * <p>
 * Интерфейс живёт в {@code core}, а не в {@code account}, чтобы зависимость шла
 * в правильную сторону: домен реализует порт ядра, ядро ничего не знает о домене.
 * Реализация — {@code dn.marketplace.account.AccountFacade} (задача B5).
 */
@FunctionalInterface
public interface AccountProvisioner {

    /**
     * Идемпотентно создаёт аккаунт с заданным id, если его ещё нет.
     * <p>
     * Контракт из SCENARIOS.md, сценарий 1: ровно один
     * {@code INSERT ... ON CONFLICT DO NOTHING}, без предварительного SELECT
     * и без чтения сущности. Метод вызывается на каждом аутентифицированном
     * запросе, поэтому обязан быть дешёвым и не брать блокировок.
     *
     * @param accountId значение claim {@code sub} из JWT Keycloak
     */
    void provision(UUID accountId);
}
