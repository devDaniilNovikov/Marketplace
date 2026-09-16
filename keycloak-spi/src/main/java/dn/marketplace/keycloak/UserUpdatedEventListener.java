package dn.marketplace.keycloak;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

/**
 * Слушает события профиля Keycloak и публикует JSON в Redis.
 * Поля совпадают с {@code UserUpdatedEvent} монолита (без Jackson, чтобы не тащить его в SPI).
 */
public class UserUpdatedEventListener implements EventListenerProvider {

    private static final Set<EventType> PROFILE_EVENTS = Set.of(
            EventType.UPDATE_PROFILE,
            EventType.UPDATE_EMAIL,
            EventType.REGISTER);

    private final JedisPool pool;
    private final String channel;
    private final KeycloakSession session;

    public UserUpdatedEventListener(JedisPool pool, String channel) {
        this(pool, channel, null);
    }

    UserUpdatedEventListener(JedisPool pool, String channel, KeycloakSession session) {
        this.pool = pool;
        this.channel = channel;
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null || event.getType() == null || !PROFILE_EVENTS.contains(event.getType())) {
            return;
        }
        String userId = event.getUserId();
        if (userId == null) {
            return;
        }
        String username = detail(event, "username");
        String email = detail(event, "email");
        String firstName = detail(event, "first_name");
        String lastName = detail(event, "last_name");
        if (session != null && event.getRealmId() != null) {
            var realm = session.realms().getRealm(event.getRealmId());
            if (realm != null) {
                UserModel user = session.users().getUserById(realm, userId);
                if (user != null) {
                    username = user.getUsername();
                    email = user.getEmail();
                    firstName = user.getFirstName();
                    lastName = user.getLastName();
                }
            }
        }
        publish(userId, username, email, firstName, lastName);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Админские правки профиля приходят как AdminEvent; воркер сверки G4 покроет пропуски.
    }

    @Override
    public void close() {
    }

    private void publish(String accountId, String username, String email, String firstName, String lastName) {
        String json = """
                {"accountId":"%s","username":%s,"email":%s,"firstName":%s,"lastName":%s}
                """.formatted(
                accountId,
                jsonString(username),
                jsonString(email),
                jsonString(firstName),
                jsonString(lastName));
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, json);
        }
    }

    private static String detail(Event event, String key) {
        if (event.getDetails() == null) {
            return null;
        }
        return event.getDetails().get(key);
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
