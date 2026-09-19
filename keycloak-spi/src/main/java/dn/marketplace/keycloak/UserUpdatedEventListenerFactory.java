package dn.marketplace.keycloak;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Фабрика SPI: публикует USER_UPDATED в Redis Pub/Sub с HMAC.
 * Канон JSON — {@code UserUpdatedEventMac}; секрет — {@code KEYCLOAK_EVENTS_MAC_SECRET}.
 */
public class UserUpdatedEventListenerFactory implements EventListenerProviderFactory {

    public static final String ID = "marketplace-user-updated";

    private JedisPool pool;
    private String channel;
    private String macSecret;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserUpdatedEventListener(pool, channel, macSecret, session);
    }

    @Override
    public void init(Config.Scope config) {
        String host = envOr("REDIS_HOST", config.get("redisHost", "redis"));
        int port = Integer.parseInt(envOr("REDIS_PORT", config.get("redisPort", "6379")));
        channel = envOr("KEYCLOAK_EVENTS_CHANNEL", config.get("channel", "keycloak.events.user"));
        macSecret = envOr("KEYCLOAK_EVENTS_MAC_SECRET", config.get("macSecret", ""));
        if (macSecret.isBlank()) {
            throw new IllegalStateException("KEYCLOAK_EVENTS_MAC_SECRET не задан — SPI не публикует USER_UPDATED без HMAC");
        }
        pool = new JedisPool(new JedisPoolConfig(), host, port);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.close();
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    private static String envOr(String env, String fallback) {
        String value = System.getenv(env);
        return value == null || value.isBlank() ? fallback : value;
    }
}
