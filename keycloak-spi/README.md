# Keycloak SPI: USER_UPDATED → Redis Pub/Sub

Контракт сообщения совпадает с `dn.marketplace.account.api.event.UserUpdatedEvent`:

```json
{"accountId":"<uuid>","username":"...","email":"...","firstName":"...","lastName":"..."}
```

Канал: `KEYCLOAK_EVENTS_CHANNEL` (по умолчанию `keycloak.events.user`).

Сборка:

```bash
./gradlew :keycloak-spi:jar
cp keycloak-spi/build/libs/marketplace-keycloak-user-events.jar docker/keycloak/providers/
```

В realm включить listener `marketplace-user-updated`. Redis для SPI: `REDIS_HOST`/`REDIS_PORT`
(в compose — сервис `redis`).
