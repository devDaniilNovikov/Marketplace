package dn.marketplace.account.api.event;

import java.util.UUID;

/**
 * Контракт Redis Pub/Sub канала {@code marketplace.keycloak.events-channel}.
 * Пишет Keycloak SPI (B8.2), читает консьюмер домена account (B8.1).
 */
public record UserUpdatedEvent(
        UUID accountId,
        String username,
        String email,
        String firstName,
        String lastName) {
}
