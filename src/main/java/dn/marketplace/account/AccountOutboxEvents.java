package dn.marketplace.account;

import java.util.Map;
import java.util.UUID;

public final class AccountOutboxEvents {

    public static final String AGGREGATE = "account";
    public static final String SELLER_ROLE_GRANTED = "SELLER_ROLE_GRANTED";
    public static final String SELLER_ROLE_REVOKED = "SELLER_ROLE_REVOKED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_ENABLED = "ACCOUNT_ENABLED";
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";

    private AccountOutboxEvents() {
    }

    public static Map<String, UUID> payload(UUID accountId) {
        return Map.of("accountId", accountId);
    }
}
