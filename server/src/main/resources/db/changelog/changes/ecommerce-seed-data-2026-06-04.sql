-- liquibase formatted sql

-- changeset andrii:13-seed-data-v2
-- comment: Розширене наповнення бази даних з покриттям edge-кейсів бізнес-логіки

-- 1. USERS (1 Admin, 5 Merchants, 9 Buyers)
-- Пароль: '12345678'
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
(10, 'marina.shop@test.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(11, 'alex.gamer@gmail.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(12, 'nina.fashion@mail.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(13, 'dmytro.dev@dev.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(14, 'kateryna.art@art.com', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER'),
(15, 'taras.sport@sport.ua', '$2b$10$uHg060FsNFvvqFGECw2ko.E9VuYhvdbH6UxoR9QVKTM10OIOMQAxS', 'BUYER');

-- 2. STORES & REQUISITES
INSERT INTO stores (id, owner_id, name, slug, address, status, rating) VALUES 
(1, 2, 'Apple Authorised', 'apple-reseller', 'м. Київ, вул. Хрещатик, 1', 'ACTIVE', 4.9),
(2, 3, 'Samsung Official', 'samsung-official', 'м. Харків, пр-т Науки, 12', 'ACTIVE', 4.7),
(3, 4, 'Nike Ukraine', 'nike-ua', 'м. Львів, площа Ринок, 5', 'ACTIVE', 4.8),
(4, 5, 'Книжковий Острів', 'book-island', 'м. Дніпро, вул. Поля, 22', 'ACTIVE', 4.2),
(5, 6, 'Fresh Coffee', 'fresh-coffee', 'м. Одеса, вул. Дерибасівська', 'ACTIVE', 4.5);

INSERT INTO merchant_requisites (id, owner_id, edrpou, iban, bank_name, is_primary) VALUES 
(1, 2, '12345678', 'UA123456789012345678901234567', 'ПриватБанк', TRUE),
(2, 3, '87654321', 'UA876543210987654321098765432', 'Монобанк', TRUE),
(3, 4, '11223344', 'UA112233445566778899001122334', 'Ощадбанк', TRUE),
(4, 5, '44332211', 'UA443322110099887766554433221', 'Райффайзен', TRUE),
(5, 6, '55667788', 'UA556677889900112233445566778', 'Укрсиббанк', TRUE);

-- 3. SHIPPING CARRIERS & STORE METHODS
INSERT INTO shipping_carriers (id, name, code, is_active) VALUES 
(1, 'Нова Пошта', 'nova_poshta', TRUE),
(2, 'Укрпошта', 'ukrposhta', TRUE),
(3, 'Meest Express', 'meest', TRUE);

INSERT INTO store_shipping_methods (store_id, carrier_id, is_enabled) VALUES 
(1, 1, TRUE), (2, 1, TRUE), (2, 2, TRUE), (3, 1, TRUE), (3, 3, TRUE),
(4, 1, TRUE), (4, 2, TRUE), (5, 1, TRUE);

-- 4. PROMO CODES
INSERT INTO promo_codes (id, code, discount_type, value, start_date, end_date, usage_limit, used_count, is_active) VALUES 
(1, 'WELCOME10', 'PERCENTAGE', 10.00, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '30 days', 1000, 2, TRUE),
(2, 'MINUS500', 'FIXED', 500.00, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '10 days', 50, 1, TRUE),
(3, 'SUMMER2026', 'PERCENTAGE', 15.00, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '15 days', 200, 0, TRUE),
(4, 'EXPIRED_CODE', 'FIXED', 200.00, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day', 100, 100, FALSE);

-- 5. CATEGORIES
INSERT INTO categories (id, name, parent_id) VALUES 
(1, 'Електроніка', NULL), (2, 'Смартфони', 1), (3, 'Ноутбуки', 1),
(4, 'Одяг та взуття', NULL), (5, 'Кросівки', 4), (6, 'Дім та затишок', NULL),
(7, 'Кава', 6), (8, 'Книги', NULL);

--- 6. PRODUCTS (30 Products для тестування пагінації)
INSERT INTO products (id, store_id, category_id, title, description, stock_quantity, standard_price, group_price, group_target_size, status, images) VALUES 
(1, 1, 2, 'iPhone 15 Pro 256GB', 'Titanium Black', 15, 52000.00, 48000.00, 3, 'ACTIVE', '[
    "https://res.cloudinary.com/dnstwasef/image/upload/v1781358986/uptyxvnip82t5a6qcof1.jpg",
    "https://res.cloudinary.com/dnstwasef/image/upload/v1781359016/w5ktp5zsmzgwbl2ehvft.jpg",
    "https://res.cloudinary.com/dnstwasef/image/upload/v1781359050/qjf9xkeg5u9clsztfmod.jpg"
]'),
(2, 1, 3, 'MacBook Pro M3 14"', '18GB RAM, 512GB SSD', 5, 85000.00, 80000.00, 2, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359125/onazvzv0kggyp8tkx7di.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359144/uei9zssl9cvzxw7owh6s.jpg"]'),
(3, 2, 2, 'Samsung Galaxy S24 Ultra', 'Titanium Gray', 20, 50000.00, 46000.00, 3, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359232/bkktvoaqe4echqxqq1bf.webp", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359246/cnsrdsjx5emc4etlpcmn.jpg"]'),
(4, 2, 1, 'Samsung Galaxy Watch 6', '44mm, Graphite', 30, 12000.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359295/ajzzfcxyaernhvpa06la.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359309/g09yjf6qlng3cqftjd2n.webp"]'),
(5, 3, 5, 'Nike Air Force 1', 'White, Size 42', 50, 4500.00, 4000.00, 5, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359393/qwnufa5mj7j5mrt4d1nx.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359414/hyzsc9qqwdzvcgtnr0td.webp"]'),
(6, 3, 5, 'Nike Dunk Low', 'Panda, Size 40', 0, 5200.00, NULL, NULL, 'OUT_OF_STOCK', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359486/n78j9gcbj6bndzzoqhpf.webp", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359507/ry3fltm6nh8veuvkvdpb.webp"]'),
(7, 3, 4, 'Nike Sportswear Tech Fleece', 'Black Hoodie', 25, 4800.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359553/rnh5mef7xik7ngw06d1r.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359568/zsqr5ggmkn4ixuaegrtk.jpg"]'),
(8, 4, 8, 'Дюна. Френк Герберт', 'Тверда обкладинка', 100, 650.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359603/raq1q21k7szhihlcnj7r.png"]'),
(9, 4, 8, '1984. Джордж Оруелл', 'Ексклюзивне видання', 80, 450.00, 350.00, 10, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359646/w5m6hyuvug811mhit2ag.jpg"]'),
(10, 5, 7, 'Кава Ефіопія 1кг', '100% Arabica', 40, 950.00, 800.00, 4, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359678/btnfhfyuc7pdhyqij7uq.webp"]'),
(11, 5, 7, 'Кава Колумбія 500г', 'Decaf', 20, 600.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359712/axb2fayhauy37gaahplv.webp"]'),
(12, 1, 2, 'iPhone 13 128GB', 'Old model', 0, 25000.00, NULL, NULL, 'ARCHIVED', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359749/lerbrvhfiwem1nmbn3fe.webp", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359761/vb5qsjup7f81afmikxax.png"]'),
(13, 2, 3, 'Samsung Galaxy Book 3', 'i7, 16GB', 10, 45000.00, 42000.00, 2, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359809/peim2izcdy9swz9r5ryo.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359823/iexdsza5izffbndl6wvb.jpg"]'),
(14, 4, 8, 'Clean Code. Robert Martin', 'IT Books', 15, 1200.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359855/okqawxnx6qe4nly5p1xd.jpg"]'),
(15, 5, 7, 'Дріп-кава Мікс', 'Упаковка 10 шт', 150, 300.00, 250.00, 5, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359891/adzl6tgenluqe901kgkk.webp"]'),
(16, 1, 3, 'iPad Pro 11" M4', 'Wi-Fi 256GB', 12, 45000.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359926/erpmkyfxi7hyhlux9lbx.webp", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359941/pjpdqd5jn7rekrgyrfdh.jpg"]'),
(17, 1, 2, 'iPhone 14 128GB', 'Midnight', 25, 30000.00, 28000.00, 5, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781359976/fppucrxpa1dcmb6z0xmu.jpg", "https://res.cloudinary.com/dnstwasef/image/upload/v1781359995/nsof2fhzxzk0rvcenhy4.jpg"]'),
(18, 2, 2, 'Samsung Galaxy A55', 'Awesome Navy', 40, 18000.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360021/lylopjmt564kp7axfhon.jpg"]'),
(19, 2, 1, 'Samsung Galaxy Buds 2 Pro', 'White', 35, 6000.00, 5500.00, 4, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360060/iavqhh6oewudvyswypbd.png", "https://res.cloudinary.com/dnstwasef/image/upload/v1781360071/l5k2aqudqdanfhk6wjvg.png"]'),
(20, 3, 5, 'Nike Air Max 270', 'Black/White', 20, 5500.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360099/jqvobecwybnnepuu3ojd.webp"]'),
(21, 3, 4, 'Nike Dri-FIT T-Shirt', 'Training shirt', 100, 1200.00, 1000.00, 10, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360130/miwpc3qa9pfbn5ezktvk.webp"]'),
(22, 4, 8, 'Design Patterns (GoF)', 'Erich Gamma', 10, 1500.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360157/zmstwbabmo6fgerua1xq.webp"]'),
(23, 4, 8, 'Grokking Algorithms', 'Aditya Bhargava', 30, 900.00, 800.00, 5, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360186/xwhqigqkabjfcul6pjbd.jpg"]'),
(24, 5, 7, 'Кава Бразилія 1кг', 'Сантос, 100% Арабіка', 60, 850.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360212/wbgf3ac4weimwnyrtbq9.webp"]'),
(25, 5, 7, 'Фільтр-пакети Hario V60', '100 шт', 200, 250.00, 200.00, 10, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360241/pqw98mpoa7dindvxaxmv.webp"]'),
(26, 1, 3, 'MacBook Air M2 13"', '8GB/256GB Space Gray', 18, 42000.00, 40000.00, 3, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360299/jpzgmfglminnoepoy7do.jpg"]'),
(27, 2, 3, 'Samsung Odyssey G5', '27" Monitor', 8, 14000.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360328/tyeg3zcftk6vz3bgp4s8.webp"]'),
(28, 3, 5, 'Nike Pegasus 40', 'Running shoes', 45, 4800.00, 4200.00, 4, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360354/lb3qppmiesk9z5d6p2xc.webp"]'),
(29, 4, 8, 'Майстер і Маргарита', 'Подарункове видання', 25, 700.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360396/f1zjcgpnkq7iadvmbgg8.jpg"]'),
(30, 5, 7, 'Кава Гватемала 250г', 'Спешелті', 30, 450.00, NULL, NULL, 'ACTIVE', '["https://res.cloudinary.com/dnstwasef/image/upload/v1781360434/l1f1dd7kq3lntoc1xxrv.jpg"]');

-- 7. GROUP BUY SESSIONS (Different statuses)
INSERT INTO group_buy_sessions (id, uuid, product_id, initiator_id, status, locked_price, locked_target_size, expires_at) VALUES 
(1, 'gb-uuid-0001', 1, 7, 'ACTIVE', 48000.00, 3, CURRENT_TIMESTAMP + INTERVAL '24 hours'),
(2, 'gb-uuid-0002', 3, 8, 'COMPLETED', 46000.00, 3, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(3, 'gb-uuid-0003', 5, 9, 'CANCELLED', 4000.00, 5, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(4, 'gb-uuid-0004', 15, 10, 'ACTIVE', 250.00, 5, CURRENT_TIMESTAMP + INTERVAL '12 hours'),
(5, 'gb-uuid-0005', 9, 11, 'COMPLETED', 350.00, 10, CURRENT_TIMESTAMP - INTERVAL '5 days');

-- 8. GROUP MEMBERS
-- Active session (2/3 members)
INSERT INTO group_members (session_id, user_id) VALUES (1, 7), (1, 12);
-- Completed session (3/3 members)
INSERT INTO group_members (session_id, user_id) VALUES (2, 8), (2, 13), (2, 14);
-- Cancelled session (2/5 members)
INSERT INTO group_members (session_id, user_id) VALUES (3, 9), (3, 15);

-- 9. ORDERS 
-- Охоплюємо різні статуси доставки, методи оплати та типи покупок
INSERT INTO orders (
    id, purchase_type, session_id, user_id, store_id, 
    buyer_name, buyer_surname, buyer_phone, buyer_email, 
    delivery_type, carrier_id, delivery_address, tracking_number, payment_method, 
    subtotal, tax, discount, total_amount, payment_status, shipping_status
) VALUES 
-- Успішне звичайне замовлення з промокодом WELCOME10 (10%)
(1, 'REGULAR', NULL, 7, 3, 'Іван', 'Покупець', '+380991234567', 'ivan@gmail.com', 'BRANCH', 1, '{"city": "Київ", "branch": "Відділення №5"}', '59000123456', 'ONLINE_CARD', 9000.00, 0.00, 900.00, 8100.00, 'PAID', 'DELIVERED'),
-- Звичайне замовлення післяплата
(2, 'REGULAR', NULL, 11, 4, 'Алекс', 'Геймер', '+380501112233', 'alex@gmail.com', 'COURIER', 2, '{"city": "Одеса", "street": "Дерибасівська, 1"}', NULL, 'CASH_ON_DELIVERY', 1200.00, 0.00, 0.00, 1200.00, 'PENDING', 'PROCESSING'),
-- Самовивіз
(3, 'REGULAR', NULL, 12, 1, 'Ніна', 'Фешн', '+380679998877', 'nina@mail.com', 'PICKUP', NULL, NULL, NULL, 'ONLINE_CARD', 85000.00, 0.00, 0.00, 85000.00, 'PAID', 'PENDING'),
-- Скасоване через Refund
(4, 'REGULAR', NULL, 15, 2, 'Тарас', 'Спорт', '+380634445566', 'taras@sport.ua', 'BRANCH', 1, '{"city": "Львів", "branch": "Відділення №12"}', '59000765432', 'ONLINE_CARD', 12000.00, 0.00, 500.00, 11500.00, 'REFUNDED', 'CANCELLED'),

-- Замовлення з успішної групової покупки (Session ID 2)
(5, 'GROUP_BUY', 2, 8, 2, 'Олена', 'П', '+380671234567', 'olena@ukr.net', 'BRANCH', 1, '{"city": "Дніпро", "branch": "№2"}', '59000888888', 'ONLINE_CARD', 46000.00, 0.00, 0.00, 46000.00, 'PAID', 'SHIPPED'),
(6, 'GROUP_BUY', 2, 13, 2, 'Дмитро', 'Дев', '+380998887766', 'dmytro@dev.ua', 'PICKUP', NULL, NULL, NULL, 'CASH_ON_DELIVERY', 46000.00, 0.00, 0.00, 46000.00, 'PENDING', 'PENDING'),
(7, 'GROUP_BUY', 2, 14, 2, 'Катерина', 'Арт', '+380507776655', 'katya@art.com', 'BRANCH', 2, '{"city": "Київ", "branch": "№14"}', NULL, 'ONLINE_CARD', 46000.00, 0.00, 0.00, 46000.00, 'PENDING', 'PROCESSING');

-- 10. ORDER ITEMS
INSERT INTO order_items (id, order_id, product_id, quantity, price_snapshot, title_snapshot, image_snapshot) VALUES 
(1, 1, 5, 2, 4500.00, 'Nike Air Force 1', 'nike_af1.jpg'), -- Разом 9000
(2, 2, 14, 1, 1200.00, 'Clean Code. Robert Martin', 'cleancode.jpg'),
(3, 3, 2, 1, 85000.00, 'MacBook Pro M3 14"', 'mbp.jpg'),
(4, 4, 4, 1, 12000.00, 'Samsung Galaxy Watch 6', 'watch6.jpg'),
(5, 5, 3, 1, 46000.00, 'Samsung Galaxy S24 Ultra', 's24.jpg'),
(6, 6, 3, 1, 46000.00, 'Samsung Galaxy S24 Ultra', 's24.jpg'),
(7, 7, 3, 1, 46000.00, 'Samsung Galaxy S24 Ultra', 's24.jpg');

-- 11. PROMO USAGE HISTORY
INSERT INTO promo_usage_history (id, promo_id, user_id, order_id) VALUES 
(1, 1, 7, 1),
(2, 2, 15, 4);

-- 12. TRANSACTION EVENTS
INSERT INTO transaction_events (id, idempotency_key, order_id, event_type, amount) VALUES 
(1, 'liqpay-1001', 1, 'PAYMENT', 8100.00),
(2, 'liqpay-1002', 3, 'PAYMENT', 85000.00),
(3, 'liqpay-1003', 4, 'PAYMENT', 11500.00),
(4, 'liqpay-refund-1003', 4, 'REFUND', 11500.00), -- Повернення коштів для Refunded замовлення
(5, 'liqpay-1004', 5, 'PAYMENT', 46000.00),
(6, 'liqpay-failed-1005', 7, 'FAILED', 46000.00); -- Користувач 14 пробував оплатити, але не вистачило коштів

-- 13. SYNCHRONIZE SEQUENCES
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('stores_id_seq', (SELECT COALESCE(MAX(id), 1) FROM stores));
SELECT setval('merchant_requisites_id_seq', (SELECT COALESCE(MAX(id), 1) FROM merchant_requisites));
SELECT setval('shipping_carriers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM shipping_carriers));
SELECT setval('store_shipping_methods_id_seq', (SELECT COALESCE(MAX(id), 1) FROM store_shipping_methods));
SELECT setval('promo_codes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM promo_codes));
SELECT setval('promo_usage_history_id_seq', (SELECT COALESCE(MAX(id), 1) FROM promo_usage_history));
SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories));
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 1) FROM products));
SELECT setval('carts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM carts));
SELECT setval('cart_items_id_seq', (SELECT COALESCE(MAX(id), 1) FROM cart_items));
SELECT setval('group_buy_sessions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM group_buy_sessions));
SELECT setval('group_members_id_seq', (SELECT COALESCE(MAX(id), 1) FROM group_members));
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 1) FROM orders));
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 1) FROM order_items));
SELECT setval('transaction_events_id_seq', (SELECT COALESCE(MAX(id), 1) FROM transaction_events));

-- rollback TRUNCATE TABLE transaction_events, promo_usage_history, order_items, orders, group_members, group_buy_sessions, cart_items, carts, products, categories, promo_codes, store_shipping_methods, shipping_carriers, merchant_requisites, stores, users RESTART IDENTITY CASCADE;