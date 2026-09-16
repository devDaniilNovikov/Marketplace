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

Listener `marketplace-user-updated` и события `REGISTER`, `UPDATE_PROFILE`, `UPDATE_EMAIL` уже
включены в `docker/keycloak/realm-export.json` — после копирования jar достаточно
`docker compose up -d keycloak` (realm импортируется только в пустую БД Keycloak; на уже
поднятом инстансе включить listener в Realm settings → Events). Без jar Keycloak лишь
пишет warning о неизвестном listener.

Redis для SPI: `REDIS_HOST`/`REDIS_PORT` (по умолчанию `redis:6379` — сервис compose).
Публикация происходит после коммита транзакции Keycloak; падение Redis логируется и не
ломает запрос пользователя (fire-and-forget, сверка — задача G4).
