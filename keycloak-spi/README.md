# Keycloak SPI: USER_UPDATED → Redis Pub/Sub

Контракт на канале — доменные поля плюс HMAC. Listener монолита отбрасывает JSON без
валидной MAC **до** `insertIfAbsent`. Канон для подписи (порядок полей фиксирован):

```json
{"accountId":"<uuid>","username":"...","email":"...","firstName":"...","lastName":"...","issuedAt":<epoch-millis>}
```

На канал уходит тот же объект с полем `mac` (hex HMAC-SHA256 канона, секрет UTF-8).
`issuedAt` принимается только внутри `marketplace.keycloak.events-mac-skew` (по умолчанию 30s).

Секрет: `KEYCLOAK_EVENTS_MAC_SECRET` (у SPI и монолита один и тот же, не в исходниках).
Локальный compose подставляет `marketplace-dev-events-mac`, если переменная не задана;
монолит на хосте: тот же env в `application.yml` (`marketplace.keycloak.events-mac-secret`).

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
ломает запрос пользователя (fire-and-forget, сверка — задача G4). HMAC — идентичность
издателя; пароль Redis его не заменяет.
