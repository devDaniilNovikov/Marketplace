--liquibase formatted sql

--changeset dn.marketplace:C1-product-schema
--comment: Initial schema for Product and Inventory domains
--rollback: DROP TABLE market_place.product_price_history; DROP TABLE market_place.product_inventory; DROP TABLE market_place.products;

-- 1. Таблица основных данных о товаре
CREATE TABLE market_place.products (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    current_price DECIMAL(19, 4) NOT NULL CHECK (current_price >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES market_place.accounts(id)
);

-- Индекс для поиска товаров конкретного продавца
CREATE INDEX idx_products_seller ON market_place.products(seller_id);
-- Индекс для витрины (активные товары)
CREATE INDEX idx_products_active ON market_place.products(id) WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- 2. Таблица склада (Inventory)
-- Вынесена в отдельную таблицу, чтобы избежать блокировок основной таблицы products при резерве
CREATE TABLE market_place.product_inventory (
    product_id UUID PRIMARY KEY,
    available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved BIGINT NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    quarantine BIGINT NOT NULL DEFAULT 0 CHECK (quarantine >= 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES market_place.products(id) ON DELETE CASCADE
);

-- 3. Лог истории цен
CREATE TABLE market_place.product_price_history (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    price DECIMAL(19, 4) NOT NULL CHECK (price >= 0),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    
    CONSTRAINT fk_price_history_product FOREIGN KEY (product_id) REFERENCES market_place.products(id) ON DELETE CASCADE
);

CREATE INDEX idx_price_history_product_date ON market_place.product_price_history(product_id, effective_from DESC);

-- 4. Триггер для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION market_place.fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON market_place.products
    FOR EACH ROW EXECUTE FUNCTION market_place.fn_update_timestamp();

CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON market_place.product_inventory
    FOR EACH ROW EXECUTE FUNCTION market_place.fn_update_timestamp();
