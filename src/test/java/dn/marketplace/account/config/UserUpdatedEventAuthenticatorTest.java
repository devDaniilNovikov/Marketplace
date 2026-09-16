package dn.marketplace.account.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserUpdatedEventAuthenticatorTest {

    private static final Instant NOW = Instant.parse("2026-09-16T18:00:00Z");
    private static final Duration SKEW = Duration.ofSeconds(30);
    private static final String FIXTURE_MAC_KEY = "fixture-mac-key";
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UserUpdatedEventAuthenticator authenticator =
            new UserUpdatedEventAuthenticator(FIXTURE_MAC_KEY, SKEW, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void валидная_mac_внутри_окна() {
        assertThat(authenticator.accept(signed(NOW.toEpochMilli(), FIXTURE_MAC_KEY))).isTrue();
    }

    @Test
    void граница_skew_включительно() {
        assertThat(authenticator.accept(signed(NOW.minus(SKEW).toEpochMilli(), FIXTURE_MAC_KEY))).isTrue();
        assertThat(authenticator.accept(signed(NOW.plus(SKEW).toEpochMilli(), FIXTURE_MAC_KEY))).isTrue();
    }

    @Test
    void за_границей_skew_отклоняется() {
        assertThat(authenticator.accept(signed(NOW.minus(SKEW).minusMillis(1).toEpochMilli(), FIXTURE_MAC_KEY))).isFalse();
        assertThat(authenticator.accept(signed(NOW.plus(SKEW).plusMillis(1).toEpochMilli(), FIXTURE_MAC_KEY))).isFalse();
    }

    @Test
    void чужая_mac_отклоняется() {
        assertThat(authenticator.accept(signed(NOW.toEpochMilli(), "foreign-secret"))).isFalse();
    }

    @Test
    void без_mac_и_issuedAt_отклоняется() {
        assertThat(authenticator.accept(new UserUpdatedWireEvent(
                ACCOUNT_ID, "alice", "a@b.c", "A", "B", NOW.toEpochMilli(), null))).isFalse();
        assertThat(authenticator.accept(new UserUpdatedWireEvent(
                ACCOUNT_ID, "alice", "a@b.c", "A", "B", null, "abcd"))).isFalse();
        assertThat(authenticator.accept(new UserUpdatedWireEvent(
                null, "alice", "a@b.c", "A", "B", NOW.toEpochMilli(), "abcd"))).isFalse();
        assertThat(authenticator.accept(null)).isFalse();
    }

    private static UserUpdatedWireEvent signed(long issuedAt, String secret) {
        String canonical = UserUpdatedEventMac.canonicalJson(
                ACCOUNT_ID.toString(), "alice", "a@b.c", "A", "B", issuedAt);
        String mac = UserUpdatedEventMac.macHex(secret, canonical);
        return new UserUpdatedWireEvent(ACCOUNT_ID, "alice", "a@b.c", "A", "B", issuedAt, mac);
    }
}
