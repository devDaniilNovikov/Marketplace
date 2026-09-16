package dn.marketplace.keycloak;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Фабрика SPI: публикует USER_UPDATED в Redis Pub/Sub.
 * Контракт JSON — {@code dn.marketplace.account.api.event.UserUpdatedEvent}.
 */
public class UserUpdatedEventListenerFactory implements EventListenerProviderFactory {

    public static final String ID = "marketplace-user-updated";

    private JedisPool pool;
    private String channel;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserUpdatedEventListener(pool, channel, session);
    }

    @Override
    public void init(Config.Scope config) {
        String host = envOr("REDIS_HOST", config.get("redisHost", "redis"));
        int port = Integer.parseInt(envOr("REDIS_PORT", config.get("redisPort", "6379")));
        channel = envOr("KEYCLOAK_EVENTS_CHANNEL", config.get("channel", "keycloak.events.user"));
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
