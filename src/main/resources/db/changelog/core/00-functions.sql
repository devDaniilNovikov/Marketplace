--liquibase formatted sql

--changeset market:00-fn-set-updated-at splitStatements:false
--comment: Общая триггерная функция для всех доменов. updated_at проставляет БД, а не приложение, чтобы значение не зависело от таймзоны JVM. Правило MEMORY.md: для функций и триггеров обязателен splitStatements:false.
CREATE OR REPLACE FUNCTION market_place.set_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
--rollback DROP FUNCTION IF EXISTS market_place.set_updated_at();
