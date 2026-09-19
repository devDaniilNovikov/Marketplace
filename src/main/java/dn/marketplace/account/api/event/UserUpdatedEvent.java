package dn.marketplace.account.api.event;

import java.util.UUID;

/**
 * Доменный payload USER_UPDATED после проверки MAC в Redis-listener.
 * На канале дополнительно {@code issuedAt} (epoch millis) и {@code mac} (HMAC-SHA256 канона);
 * listener отбрасывает сообщение до {@code UserUpdatedHandler}, если MAC нет или она чужая.
 */
public record UserUpdatedEvent(
        UUID accountId,
        String username,
        String email,
        String firstName,
        String lastName) {
}
