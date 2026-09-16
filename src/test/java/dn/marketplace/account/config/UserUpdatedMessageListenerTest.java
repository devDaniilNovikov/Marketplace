package dn.marketplace.account.config;

import dn.marketplace.account.api.event.UserUpdatedEvent;
import dn.marketplace.account.service.UserUpdatedHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserUpdatedMessageListenerTest {

    private static final Instant NOW = Instant.parse("2026-09-16T18:00:00Z");
    private static final String FIXTURE_MAC_KEY = "fixture-mac-key";
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final byte[] CHANNEL = "keycloak.events.user".getBytes(StandardCharsets.UTF_8);

    @Mock
    UserUpdatedHandler handler;

    UserUpdatedMessageListener listener;

    @BeforeEach
    void setUp() {
        UserUpdatedEventAuthenticator authenticator = new UserUpdatedEventAuthenticator(
                FIXTURE_MAC_KEY, Duration.ofSeconds(30), Clock.fixed(NOW, ZoneOffset.UTC));
        listener = new UserUpdatedMessageListener(new JsonMapper(), authenticator, handler);
    }

    @Test
    void без_mac_не_вызывает_handler() {
        String unsigned = "{\"accountId\":\"" + ACCOUNT_ID + "\",\"username\":\"alice\","
                + "\"email\":\"a@b.c\",\"firstName\":\"A\",\"lastName\":\"B\"}";
        listener.onMessage(message(unsigned), CHANNEL);
        verify(handler, never()).handle(any());
    }

    @Test
    void чужая_mac_не_вызывает_handler() {
        String body = UserUpdatedEventMac.wireJson(
                ACCOUNT_ID.toString(), "alice", "a@b.c", "A", "B", NOW.toEpochMilli(), "foreign-secret");
        listener.onMessage(message(body), CHANNEL);
        verify(handler, never()).handle(any());
    }

    @Test
    void валидная_mac_передаёт_доменное_событие() {
        String body = UserUpdatedEventMac.wireJson(
                ACCOUNT_ID.toString(), "alice", "a@b.c", "A", "B", NOW.toEpochMilli(), FIXTURE_MAC_KEY);
        listener.onMessage(message(body), CHANNEL);

        ArgumentCaptor<UserUpdatedEvent> captor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
        verify(handler).handle(captor.capture());
        UserUpdatedEvent event = captor.getValue();
        assertThat(event.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.username()).isEqualTo("alice");
        assertThat(event.email()).isEqualTo("a@b.c");
        assertThat(event.firstName()).isEqualTo("A");
        assertThat(event.lastName()).isEqualTo("B");
    }

    @Test
    void пустое_тело_не_вызывает_handler() {
        listener.onMessage(message("{"), CHANNEL);
        verify(handler, never()).handle(any());
    }

    private static Message message(String body) {
        return new DefaultMessage(CHANNEL, body.getBytes(StandardCharsets.UTF_8));
    }
}
