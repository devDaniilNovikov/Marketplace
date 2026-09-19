package dn.marketplace.account.config;

import dn.marketplace.account.api.event.UserUpdatedEvent;

import java.util.UUID;

/**
 * Проволочный JSON канала Redis: доменные поля + {@code issuedAt} (epoch millis) + {@code mac}.
 * Проверка MAC — в {@link UserUpdatedEventAuthenticator}, не в {@code UserUpdatedHandler}.
 */
record UserUpdatedWireEvent(
        UUID accountId,
        String username,
        String email,
        String firstName,
        String lastName,
        Long issuedAt,
        String mac) {

    UserUpdatedEvent toDomain() {
        return new UserUpdatedEvent(accountId, username, email, firstName, lastName);
    }
}
