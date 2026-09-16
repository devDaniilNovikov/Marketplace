--liquibase formatted sql

--changeset market:02-product-stub
--comment: Заглушка, чтобы Hibernate ddl-auto:validate поднимал контекст до Фазы C.
CREATE TABLE market_place.products
(
    id UUID PRIMARY KEY
);
--rollback DROP TABLE IF EXISTS market_place.products;
