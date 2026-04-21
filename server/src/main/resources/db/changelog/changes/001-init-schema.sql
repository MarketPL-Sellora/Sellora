-- liquibase formatted sql

-- changeset vlad:1
-- 1. Таблиця користувачів
CREATE TABLE users (
                     id BIGSERIAL PRIMARY KEY,
                     email VARCHAR(255) UNIQUE NOT NULL,
                     password_hash VARCHAR(255) NOT NULL,
                     role VARCHAR(50) NOT NULL CHECK (role IN ('BUYER', 'MERCHANT')),
                     created_at TIMESTAMP DEFAULT now(),
                     updated_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE users;

-- changeset vlad:2
-- 2. Таблиця категорій (ієрархічна)
CREATE TABLE categories (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) UNIQUE NOT NULL,
                          category_id BIGINT REFERENCES categories(id) ON DELETE RESTRICT,
                          created_at TIMESTAMP DEFAULT now(),
                          updated_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE categories;

-- changeset vlad:3
-- 3. Таблиця товарів
CREATE TABLE products (
                        id BIGSERIAL PRIMARY KEY,
                        merchant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        attributes JSONB,
                        images JSONB,
                        stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
                        standard_price DECIMAL(10,2) NOT NULL CHECK (standard_price >= 0),
                        group_price DECIMAL(10,2) NOT NULL CHECK (group_price >= 0),
                        group_target_size INT NOT NULL CHECK (group_target_size > 1),
                        status VARCHAR(50) NOT NULL CHECK (status IN ('active', 'out of stock', 'archived')),
                        created_at TIMESTAMP DEFAULT now(),
                        updated_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE products;

-- changeset vlad:4
-- 4. Сесії групових покупок
CREATE TABLE group_buy_sessions (
                                  id BIGSERIAL PRIMARY KEY,
                                  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                  initiator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
                                  locked_price DECIMAL(10,2) NOT NULL,
                                  locked_target_size INT NOT NULL,
                                  expires_at TIMESTAMP NOT NULL,
                                  created_at TIMESTAMP DEFAULT now(),
                                  updated_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE group_buy_sessions;

-- changeset vlad:5
-- 5. Учасники груп (Join Table)
CREATE TABLE group_members (
                             id BIGSERIAL PRIMARY KEY,
                             session_id BIGINT NOT NULL REFERENCES group_buy_sessions(id) ON DELETE CASCADE,
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             joined_at TIMESTAMP DEFAULT now(),
                             UNIQUE (session_id, user_id)
);
-- rollback DROP TABLE group_members;

-- changeset vlad:6
-- 6. Замовлення
CREATE TABLE orders (
                      id BIGSERIAL PRIMARY KEY,
                      purchase_type VARCHAR(50) NOT NULL CHECK (purchase_type IN ('REGULAR', 'GROUP_BUY')),
                      session_id BIGINT REFERENCES group_buy_sessions(id) ON DELETE SET NULL,
                      user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                      merchant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                      product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
                      product_title_snapshot VARCHAR(255) NOT NULL,
                      product_image_snapshot VARCHAR(1024),
                      final_price DECIMAL(10,2) NOT NULL,
                      payment_status VARCHAR(50) NOT NULL CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED')),
                      shipping_status VARCHAR(50) NOT NULL CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED')),
                      created_at TIMESTAMP DEFAULT now(),
                      updated_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE orders;

-- changeset vlad:7
-- 7. Транзакційні події
CREATE TABLE transaction_events (
                                  id BIGSERIAL PRIMARY KEY,
                                  idempotency_key VARCHAR(255) UNIQUE NOT NULL,
                                  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                  order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
                                  event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('HOLD', 'CAPTURE', 'REFUND')),
                                  amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
                                  created_at TIMESTAMP DEFAULT now()
);
-- rollback DROP TABLE transaction_events;
