-- liquibase formatted sql

-- ==========================================
-- DOMAIN: PROMO
-- ==========================================

-- changeset andrii:18-promo-codes
CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_limit INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_promo_codes_code UNIQUE (code),
    CONSTRAINT chk_promo_codes_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT chk_promo_codes_value CHECK (
        value > 0 AND (discount_type = 'FIXED' OR (discount_type = 'PERCENTAGE' AND value <= 100))
    ),
    CONSTRAINT chk_promo_codes_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_promo_codes_used_count CHECK (usage_limit >= used_count)
);

CREATE INDEX idx_promo_codes_code ON promo_codes USING btree(code);
-- rollback DROP TABLE promo_codes CASCADE;


-- changeset andrii:19-promo-history
CREATE TABLE promo_usage_history (
    id BIGSERIAL PRIMARY KEY,
    promo_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_promo_usage_history_usage UNIQUE (promo_id, user_id, order_id),
    CONSTRAINT fk_promo_usage_history_promo FOREIGN KEY (promo_id) REFERENCES promo_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_usage_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_promo_usage_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT
);

CREATE INDEX idx_promo_usage_history_promo_id ON promo_usage_history USING btree(promo_id);
CREATE INDEX idx_promo_usage_history_user_id ON promo_usage_history USING btree(user_id);
CREATE INDEX idx_promo_usage_history_order_id ON promo_usage_history USING btree(order_id);
-- rollback DROP TABLE promo_usage_history CASCADE;


-- ==========================================
-- DOMAIN: SHIPPING LOGISTICS
-- ==========================================

-- changeset andrii:20-shipping-carriers
CREATE TABLE shipping_carriers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,

    CONSTRAINT uq_shipping_carriers_code UNIQUE (code)
);
-- rollback DROP TABLE shipping_carriers CASCADE;


-- changeset andrii:21-store-shipping-methods
CREATE TABLE store_shipping_methods (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    carrier_id BIGINT NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,

    CONSTRAINT uq_store_shipping_method UNIQUE (store_id, carrier_id),
    CONSTRAINT fk_store_shipping_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_store_shipping_carrier FOREIGN KEY (carrier_id) REFERENCES shipping_carriers(id) ON DELETE CASCADE
);
-- rollback DROP TABLE store_shipping_methods CASCADE;


-- ==========================================
-- DOMAIN: ORDERS ALTERATIONS
-- ==========================================

-- changeset andrii:22-alter-orders
-- Видаляємо старий констрейнт статусів оплати
ALTER TABLE orders DROP CONSTRAINT chk_orders_payment_status;
ALTER TABLE orders DROP CONSTRAINT chk_orders_shipping_status;

-- Додаємо нові колонки з безпечними дефолтними значеннями для існуючих записів
ALTER TABLE orders 
    ADD COLUMN buyer_name VARCHAR(255) NOT NULL DEFAULT 'Migrated',
    ADD COLUMN buyer_surname VARCHAR(255) NOT NULL DEFAULT 'User',
    ADD COLUMN buyer_phone VARCHAR(20) NOT NULL DEFAULT '+380000000000',
    ADD COLUMN buyer_email VARCHAR(255) NOT NULL DEFAULT 'migrated@system.ua',
    ADD COLUMN delivery_type VARCHAR(50) NOT NULL DEFAULT 'BRANCH',
    ADD COLUMN carrier_id BIGINT,
    ADD COLUMN delivery_address JSONB,
    ADD COLUMN tracking_number VARCHAR(100),
    ADD COLUMN payment_method VARCHAR(50) NOT NULL DEFAULT 'ONLINE_CARD',
    ADD COLUMN order_comment TEXT,
    ADD COLUMN subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN tax DECIMAL(10,2) DEFAULT 0.00,
    ADD COLUMN discount DECIMAL(10,2) DEFAULT 0.00;

-- Мігруємо існуючі фінансові дані, щоб математика зійшлася (subtotal = total_amount для старих замовлень)
UPDATE orders SET subtotal = total_amount;

-- Видаляємо дефолти
ALTER TABLE orders ALTER COLUMN buyer_name DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN buyer_surname DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN buyer_phone DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN buyer_email DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN delivery_type DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN payment_method DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN subtotal DROP DEFAULT;

-- Додаємо нові констрейнти
ALTER TABLE orders ADD CONSTRAINT chk_orders_delivery_type CHECK (delivery_type IN ('BRANCH', 'COURIER', 'PICKUP'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method CHECK (payment_method IN ('ONLINE_CARD', 'CASH_ON_DELIVERY'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('WAITING_FOR_GROUP', 'PENDING', 'PAID', 'CANCELLED', 'REFUNDED'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_financials_positive CHECK (subtotal >= 0 AND tax >= 0 AND discount >= 0 AND total_amount >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_orders_logistic_conditional_nulls CHECK (
    (delivery_type = 'PICKUP') OR (delivery_type IN ('BRANCH', 'COURIER') AND carrier_id IS NOT NULL AND delivery_address IS NOT NULL)
);

-- Додаємо зовнішні ключі та індекси
ALTER TABLE orders ADD CONSTRAINT fk_orders_carrier FOREIGN KEY (carrier_id) REFERENCES shipping_carriers(id) ON DELETE RESTRICT;
CREATE INDEX idx_orders_carrier_id ON orders USING btree(carrier_id);

-- rollback ALTER TABLE orders DROP COLUMN buyer_name, DROP COLUMN buyer_surname, DROP COLUMN buyer_phone, DROP COLUMN buyer_email, DROP COLUMN delivery_type, DROP COLUMN carrier_id, DROP COLUMN delivery_address, DROP COLUMN tracking_number, DROP COLUMN payment_method, DROP COLUMN order_comment, DROP COLUMN subtotal, DROP COLUMN tax, DROP COLUMN discount; ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED')); ALTER TABLE orders ADD CONSTRAINT chk_orders_shipping_status CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED'));

-- changeset andrii:23-alter-transactions
-- Видаляємо старий констрейнт подій (де були HOLD, CAPTURE)
ALTER TABLE transaction_events DROP CONSTRAINT chk_transactions_event_type;

-- Переводимо старі історичні записи у новий формат, щоб база не впала при створенні нового констрейнту
UPDATE transaction_events SET event_type = 'PAYMENT' WHERE event_type IN ('HOLD', 'CAPTURE');

-- Додаємо оновлений констрейнт
ALTER TABLE transaction_events ADD CONSTRAINT chk_transactions_event_type CHECK (event_type IN ('PAYMENT', 'FAILED', 'REFUND'));

-- rollback ALTER TABLE transaction_events DROP CONSTRAINT chk_transactions_event_type; UPDATE transaction_events SET event_type = 'CAPTURE' WHERE event_type = 'PAYMENT'; ALTER TABLE transaction_events ADD CONSTRAINT chk_transactions_event_type CHECK (event_type IN ('HOLD', 'CAPTURE', 'REFUND'));


