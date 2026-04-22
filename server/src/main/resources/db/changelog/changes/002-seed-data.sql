-- liquibase formatted sql

-- changeset andrii:13-seed-data
-- comment: Наповнення бази даних еталонними тестовими даними (мінімум 5 записів на таблицю)

-- 1. USERS (Admin, 5 Merchants, 4 Buyers)
-- Пароль для всіх: '12345678' (хеш BCrypt $2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS)
INSERT INTO users (id, email, password_hash, role) VALUES 
(1, 'admin@sellora.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'ADMIN'),
(2, 'apple_auth@merchant.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'MERCHANT'),
(3, 'samsung_official@merchant.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'MERCHANT'),
(4, 'nike_distributor@merchant.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'MERCHANT'),
(5, 'book_island@merchant.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'MERCHANT'),
(6, 'coffee_roasters@merchant.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'MERCHANT'),
(7, 'ivan.buyer@gmail.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(8, 'olena.p@ukr.net', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(9, 'sergiy.tech@outlook.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(10, 'marina.shop@test.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER');

-- 2. STORES
INSERT INTO stores (id, owner_id, name, slug, address, contact_phone, description, status, rating) VALUES 
(1, 2, 'Apple Authorised Reseller', 'apple-reseller', 'м. Київ, вул. Хрещатик, 1', '+380501234567', 'Apple-reseller description', 'ACTIVE', 4.9),
(2, 3, 'Samsung Official Store', 'samsung-official', 'м. Харків, пр-т Науки, 12', '+380502345678', 'Samsung Official Store description', 'ACTIVE', 4.7),
(3, 4, 'Nike Ukraine', 'nike-ua', 'м. Львів, площа Ринок, 5', '+380503456789', 'Nike Ukraine description', 'ACTIVE', 4.8),
(4, 5, 'Книжковий Острів', 'book-island', 'м. Дніпро, вул. Поля, 22', '+380504567890', 'Книжковий Острів description', 'PENDING', 0.0),
(5, 6, 'Fresh Coffee Roasters', 'fresh-coffee', 'м. Одеса, вул. Дерибасівська, 10', '+380505678901', 'Fresh Coffee Roasters description', 'ACTIVE', 4.5);

-- 3. MERCHANT REQUISITES
INSERT INTO merchant_requisites (id, owner_id, edrpou, iban, bank_name, is_primary) VALUES 
(1, 2, '12345678', 'UA123456789012345678901234567', 'ПриватБанк', TRUE),
(2, 3, '87654321', 'UA876543210987654321098765432', 'Монобанк', TRUE),
(3, 4, '11223344', 'UA112233445566778899001122334', 'Ощадбанк', TRUE),
(4, 5, '44332211', 'UA443322110099887766554433221', 'Райффайзен', TRUE),
(5, 6, '55667788', 'UA556677889900112233445566778', 'Укрсиббанк', TRUE);

-- 4. CATEGORIES
INSERT INTO categories (id, name, parent_id) VALUES 
(1, 'Електроніка', NULL),
(2, 'Смартфони', 1),
(3, 'Ноутбуки', 1),
(4, 'Одяг та взуття', NULL),
(5, 'Книги', NULL);

-- 5. PRODUCTS
INSERT INTO products (id, store_id, category_id, title, description, stock_quantity, standard_price, group_price, group_target_size, status) VALUES 
(1, 1, 2, 'iPhone 15 Pro 128GB', 'Колір: Natural Titanium', 50, 48000.00, 44000.00, 3, 'ACTIVE'),
(2, 2, 2, 'Samsung Galaxy S24 Ultra', 'З підтримкою S-Pen', 30, 52000.00, 47000.00, 3, 'ACTIVE'),
(3, 3, 4, 'Nike Air Max 270', 'Розмір 42, Чорні', 100, 5600.00, 4800.00, 5, 'ACTIVE'),
(4, 5, 1, 'Кавомолка електрична', 'Жорнова з нержавіючої сталі', 15, 3200.00, 2700.00, 4, 'ACTIVE'),
(5, 1, 3, 'MacBook Air M2 13"', '8GB RAM, 256GB SSD', 10, 42000.00, 38500.00, 2, 'ACTIVE');

-- 6. CARTS (For 4 buyers)
INSERT INTO carts (id, user_id) VALUES 
(1, 7), (2, 8), (3, 9), (4, 10);

-- 7. CART ITEMS
INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES 
(1, 1, 1, 1), (2, 1, 3, 2), (3, 2, 4, 1), (4, 3, 2, 1), (5, 4, 1, 1);

-- 8. GROUP BUY SESSIONS
INSERT INTO group_buy_sessions (id, product_id, initiator_id, status, locked_price, locked_target_size, expires_at) VALUES 
(1, 1, 7, 'ACTIVE', 44000.00, 3, CURRENT_TIMESTAMP + INTERVAL '24 hours'),
(2, 2, 8, 'ACTIVE', 47000.00, 3, CURRENT_TIMESTAMP + INTERVAL '12 hours'),
(3, 3, 9, 'COMPLETED', 48000.00, 5, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
(4, 4, 10, 'CANCELLED', 2700.00, 4, CURRENT_TIMESTAMP - INTERVAL '2 days'),
(5, 5, 7, 'ACTIVE', 38500.00, 2, CURRENT_TIMESTAMP + INTERVAL '48 hours');

-- 9. GROUP MEMBERS
INSERT INTO group_members (id, session_id, user_id) VALUES 
(1, 1, 7), (2, 1, 8), (3, 2, 8), (4, 3, 9), (5, 3, 7), (6, 3, 8), (7, 3, 10), (8, 3, 1); -- Адмін теж може бути покупцем

-- 10. ORDERS (Headers)
INSERT INTO orders (id, purchase_type, session_id, user_id, store_id, total_amount, payment_status, shipping_status) VALUES 
(1, 'GROUP_BUY', 3, 9, 3, 4800.00, 'PAID', 'SHIPPED'),
(2, 'REGULAR', NULL, 7, 1, 48000.00, 'PAID', 'DELIVERED'),
(3, 'REGULAR', NULL, 8, 5, 3200.00, 'PENDING', 'PENDING'),
(4, 'GROUP_BUY', 3, 7, 3, 4800.00, 'PAID', 'SHIPPED'),
(5, 'REGULAR', NULL, 10, 2, 52000.00, 'CANCELLED', 'PENDING');

-- 11. ORDER ITEMS (Details with snapshots)
INSERT INTO order_items (id, order_id, product_id, quantity, price_snapshot, title_snapshot, image_snapshot) VALUES 
(1, 1, 3, 1, 4800.00, 'Nike Air Max 270', 'nike_am270.jpg'),
(2, 2, 1, 1, 48000.00, 'iPhone 15 Pro 128GB', 'iphone15pro.jpg'),
(3, 3, 4, 1, 3200.00, 'Кавомолка електрична', 'grinder.jpg'),
(4, 4, 3, 1, 4800.00, 'Nike Air Max 270', 'nike_am270.jpg'),
(5, 5, 2, 1, 52000.00, 'Samsung Galaxy S24 Ultra', 's24ultra.jpg');

-- 12. TRANSACTION EVENTS
INSERT INTO transaction_events (id, idempotency_key, order_id, event_type, amount) VALUES 
(1, 'idemp-001', 1, 'CAPTURE', 4800.00),
(2, 'idemp-002', 2, 'CAPTURE', 48000.00),
(3, 'idemp-003', 4, 'CAPTURE', 4800.00),
(4, 'idemp-004', 5, 'HOLD', 52000.00),
(5, 'idemp-005', 5, 'REFUND', 52000.00);

-- SYNCHRONIZE SEQUENCES
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('stores_id_seq', (SELECT MAX(id) FROM stores));
SELECT setval('merchant_requisites_id_seq', (SELECT MAX(id) FROM merchant_requisites));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('carts_id_seq', (SELECT MAX(id) FROM carts));
SELECT setval('cart_items_id_seq', (SELECT MAX(id) FROM cart_items));
SELECT setval('group_buy_sessions_id_seq', (SELECT MAX(id) FROM group_buy_sessions));
SELECT setval('group_members_id_seq', (SELECT MAX(id) FROM group_members));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
SELECT setval('transaction_events_id_seq', (SELECT MAX(id) FROM transaction_events));

-- rollback TRUNCATE TABLE transaction_events, order_items, orders, group_members, group_buy_sessions, cart_items, carts, products, categories, merchant_requisites, stores, users RESTART IDENTITY CASCADE;