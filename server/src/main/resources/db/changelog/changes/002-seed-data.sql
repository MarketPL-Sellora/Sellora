-- liquibase formatted sql

-- changeset vlad:8
INSERT INTO users (email, password_hash, role) VALUES
                                                 ('buyer@test.com', 'hash123', 'BUYER'),
                                                 ('merchant@test.com', 'hash456', 'MERCHANT');

INSERT INTO categories (name) VALUES ('Смартфони'), ('Ноутбуки');

INSERT INTO products (merchant_id, category_id, title, stock_quantity, standard_price, group_price, group_target_size, status)
VALUES (2, 1, 'iPhone 15 Pro', 50, 45000.00, 40000.00, 3, 'active');

-- rollback TRUNCATE TABLE transaction_events, orders, group_members, group_buy_sessions, products, categories, users RESTART IDENTITY CASCADE;
