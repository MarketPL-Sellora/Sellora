-- liquibase formatted sql

-- ==========================================
-- DOMAIN: USERS & STORES
-- ==========================================

-- changeset andrii:1-users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('BUYER', 'MERCHANT', 'ADMIN'))
);
-- rollback DROP TABLE users CASCADE;


-- changeset andrii:2-stores
CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    address TEXT,
    contact_phone VARCHAR(20),
    description TEXT,
    logo_url VARCHAR(500),
    rating DECIMAL(3,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_stores_owner UNIQUE (owner_id),
    CONSTRAINT uq_stores_name UNIQUE (name),
    CONSTRAINT uq_stores_slug UNIQUE (slug),
    CONSTRAINT chk_stores_status CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_stores_rating CHECK (rating >= 0.00 AND rating <= 5.00),
    
    CONSTRAINT fk_stores_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_stores_slug ON stores USING btree(slug);
-- rollback DROP TABLE stores CASCADE;


-- changeset andrii:3-merchant-requisites
CREATE TABLE merchant_requisites (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    edrpou VARCHAR(10) NOT NULL,
    iban VARCHAR(29) NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_merchant_edrpou UNIQUE (edrpou),
    CONSTRAINT chk_merchant_iban_format CHECK (iban LIKE 'UA%' AND char_length(iban) = 29),
    
    CONSTRAINT fk_merchant_requisites_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Частковий унікальний індекс для гарантії єдиного основного рахунку
CREATE UNIQUE INDEX uq_merchant_primary_iban ON merchant_requisites USING btree(owner_id) WHERE is_primary = TRUE;
-- rollback DROP TABLE merchant_requisites CASCADE;


-- ==========================================
-- DOMAIN: CATALOG & CART
-- ==========================================

-- changeset andrii:4-categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX idx_categories_parent_id ON categories USING btree(parent_id);
-- rollback DROP TABLE categories CASCADE;


-- changeset andrii:5-products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    attributes JSONB,
    images JSONB,
    stock_quantity INTEGER NOT NULL,
    standard_price DECIMAL(10,2) NOT NULL,
    group_price DECIMAL(10,2) NOT NULL,
    group_target_size INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_prices CHECK (standard_price >= 0 AND group_price >= 0),
    CONSTRAINT chk_products_target_size CHECK (group_target_size > 1),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'OUT_OF_STOCK', 'ARCHIVED')),

    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_store_id ON products USING btree(store_id);
CREATE INDEX idx_products_category_id ON products USING btree(category_id);
CREATE INDEX idx_products_attributes ON products USING gin(attributes);
-- rollback DROP TABLE products CASCADE;


-- changeset andrii:6-carts
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- rollback DROP TABLE carts CASCADE;


-- changeset andrii:7-cart-items
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cart_items_product UNIQUE (cart_id, product_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),

    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_product_id ON cart_items USING btree(product_id);
-- rollback DROP TABLE cart_items CASCADE;


-- ==========================================
-- DOMAIN: GROUP BUY LOGIC
-- ==========================================

-- changeset andrii:8-group-buy-sessions
CREATE TABLE group_buy_sessions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    locked_price DECIMAL(10,2) NOT NULL,
    locked_target_size INTEGER NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_group_sessions_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),

    CONSTRAINT fk_sessions_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sessions_initiator FOREIGN KEY (initiator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_sessions_product_id ON group_buy_sessions USING btree(product_id);
CREATE INDEX idx_group_sessions_initiator_id ON group_buy_sessions USING btree(initiator_id);
CREATE INDEX idx_group_sessions_active_expires ON group_buy_sessions USING btree(expires_at) WHERE status = 'ACTIVE';
-- rollback DROP TABLE group_buy_sessions CASCADE;


-- changeset andrii:9-group-members
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_group_members_session_user UNIQUE (session_id, user_id),

    CONSTRAINT fk_group_members_session FOREIGN KEY (session_id) REFERENCES group_buy_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_members_session_id ON group_members USING btree(session_id);
CREATE INDEX idx_group_members_user_id ON group_members USING btree(user_id);
-- rollback DROP TABLE group_members CASCADE;


-- ==========================================
-- DOMAIN: ORDERS & TRANSACTIONS
-- ==========================================

-- changeset andrii:10-orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    purchase_type VARCHAR(50) NOT NULL,
    session_id BIGINT,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    shipping_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_orders_purchase_type CHECK (purchase_type IN ('REGULAR', 'GROUP_BUY')),
    CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_orders_shipping_status CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED')),
    CONSTRAINT chk_orders_session_integrity CHECK (purchase_type = 'REGULAR' OR (purchase_type = 'GROUP_BUY' AND session_id IS NOT NULL)),

    CONSTRAINT fk_orders_session FOREIGN KEY (session_id) REFERENCES group_buy_sessions(id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user_id ON orders USING btree(user_id);
CREATE INDEX idx_orders_store_id ON orders USING btree(store_id);
CREATE INDEX idx_orders_session_id ON orders USING btree(session_id);
-- rollback DROP TABLE orders CASCADE;


-- changeset andrii:11-order-items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_snapshot DECIMAL(10,2) NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL,
    image_snapshot VARCHAR(1024),

    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_product_id ON order_items USING btree(product_id);
-- rollback DROP TABLE order_items CASCADE;


-- changeset andrii:12-transaction-events
CREATE TABLE transaction_events (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_transactions_event_type CHECK (event_type IN ('HOLD', 'CAPTURE', 'REFUND')),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),

    CONSTRAINT fk_transactions_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT
);

CREATE INDEX idx_transactions_order_id ON transaction_events USING btree(order_id);
-- rollback DROP TABLE transaction_events CASCADE;