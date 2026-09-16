package dn.marketplace.account.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Идентичность издателя USER_UPDATED: HMAC канона + окно {@code issuedAt}.
 * Вызывается из Redis-listener до {@code UserUpdatedHandler.insertIfAbsent}.
 */
@Component
final class UserUpdatedEventAuthenticator {

    private final String secret;
    private final Duration skew;
    private final Clock clock;

    @Autowired
    UserUpdatedEventAuthenticator(UserUpdatedMacProperties properties, Clock clock) {
        this(properties.eventsMacSecret(), properties.eventsMacSkew(), clock);
    }

    UserUpdatedEventAuthenticator(String secret, Duration skew, Clock clock) {
        this.secret = secret;
        this.skew = skew;
        this.clock = clock;
    }

    boolean accept(UserUpdatedWireEvent wire) {
        if (wire == null || wire.accountId() == null || wire.issuedAt() == null) {
            return false;
        }
        Instant issued = Instant.ofEpochMilli(wire.issuedAt());
        Instant now = Instant.now(clock);
        if (issued.isBefore(now.minus(skew)) || issued.isAfter(now.plus(skew))) {
            return false;
        }
        String canonical = UserUpdatedEventMac.canonicalJson(
                wire.accountId().toString(),
                wire.username(),
                wire.email(),
                wire.firstName(),
                wire.lastName(),
                wire.issuedAt());
        return UserUpdatedEventMac.macEqualsHex(secret, canonical, wire.mac());
    }
}
