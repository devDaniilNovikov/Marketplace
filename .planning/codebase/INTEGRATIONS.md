# Integrations

**Analysis Date:** 2026-09-16

## Keycloak

- Issuer: `http://localhost:8080/realms/market-realm`
- SPI `marketplace-user-updated` → Redis канал `keycloak.events.user`
- Listener в `docker/keycloak/realm-export.json`
- Admin API (роль SELLER, disable user) — фаза 4, ещё нет клиента

## Redis

- Pub/Sub USER_UPDATED
- Кэш каталога — фаза 3 C6
- `RedisTemplate<String,Object>` + polymorphic typing whitelist `dn.marketplace`

## Postgres

- Единственная БД монолита, схема `market_place`
- Outbox JSONB, SKIP LOCKED колонки уже в `00-outbox.sql`

## MinIO / Stripe / Mail

Зависимости и контейнеры есть, доменного кода нет (фазы 3 и 6).

## Observability

Actuator prometheus; Grafana на 3001 (3000 занят приложением).

---
*Integrations analysis: 2026-09-16*
