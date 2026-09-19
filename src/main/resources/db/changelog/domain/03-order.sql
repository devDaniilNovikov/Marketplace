--liquibase formatted sql

--changeset market:03-order-stub
--comment: Заглушка, чтобы Hibernate ddl-auto:validate поднимал контекст до Фазы D.
CREATE TABLE market_place.orders
(
    id UUID PRIMARY KEY
);
--rollback DROP TABLE IF EXISTS market_place.orders;
