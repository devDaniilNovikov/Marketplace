package dn.marketplace.account;

import dn.marketplace.account.api.enums.BusinessStatus;

import java.util.UUID;

/**
 * Проекция аккаунта для других доменов. JPA-сущность наружу не отдаём.
 */
public record AccountView(
        UUID id,
        String username,
        BusinessStatus status,
        boolean banned,
        boolean deleted) {

    public boolean canTrade() {
        return !banned && !deleted;
    }

    public boolean canSell() {
        return canTrade() && status == BusinessStatus.SELLER;
    }
}
