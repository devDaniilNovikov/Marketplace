--liquibase formatted sql

--changeset market:00-schema-create
--comment: Схема монолита. Все доменные таблицы живут здесь (решение №8). Служебные таблицы Liquibase остаются в public, иначе получается курица и яйцо.
CREATE SCHEMA IF NOT EXISTS market_place;
--rollback DROP SCHEMA IF EXISTS market_place CASCADE;
