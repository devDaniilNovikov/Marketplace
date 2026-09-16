package dn.marketplace.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Слушает события профиля Keycloak и публикует JSON {@code USER_UPDATED} в Redis.
 * <p>
 * Поля совпадают с {@code UserUpdatedEvent} монолита. Значения берутся из {@link UserModel},
 * а не из {@code event.getDetails()}: ключи details отличаются по типу события
 * ({@code first_name} у REGISTER, {@code updated_first_name} у UPDATE_PROFILE), а модель
 * всегда содержит итоговое состояние профиля.
 * <p>
 * Публикация отложена до коммита транзакции Keycloak: иначе откат после {@code onEvent}
 * оставил бы в монолите «фантомное» обновление.
 */
public class UserUpdatedEventListener implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(UserUpdatedEventListener.class);

    private static final Set<EventType> PROFILE_EVENTS = Set.of(
            EventType.UPDATE_PROFILE,
            EventType.UPDATE_EMAIL,
            EventType.REGISTER);

    private final JedisPool pool;
    private final String channel;
    private final KeycloakSession session;

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
        if (userId == null || event.getRealmId() == null) {
            return;
        }
        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            return;
        }
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            LOG.warnf("USER_UPDATED пропущен: пользователь %s не найден в realm %s", userId, realm.getName());
            return;
        }

        String json;
        try {
            json = toJson(userId, user);
        } catch (IOException e) {
            LOG.errorf(e, "Не удалось сериализовать USER_UPDATED для %s", userId);
            return;
        }

        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                publish(json);
            }

            @Override
            protected void rollbackImpl() {
                // транзакция Keycloak откатилась — профиль не изменился, публиковать нечего
            }
        });
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Админские правки профиля приходят как AdminEvent; воркер сверки G4 покроет пропуски.
    }

    @Override
    public void close() {
    }

    static String toJson(String accountId, UserModel user) throws IOException {
        // LinkedHashMap, а не Map.of: значения могут быть null, порядок полей — как в контракте
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", accountId);
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getFirstName());
        payload.put("lastName", user.getLastName());
        return JsonSerialization.writeValueAsString(payload);
    }

    private void publish(String json) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, json);
        } catch (RuntimeException e) {
            // Pub/Sub — fire-and-forget (SCENARIOS.md, сценарий 2); падение Redis не должно
            // ронять запрос пользователя в Keycloak. Расхождение закроет сверка G4.
            LOG.errorf(e, "Не удалось опубликовать USER_UPDATED в канал %s", channel);
        }
    }
}
