-- liquibase formatted sql

-- changeset andrii:26-product-reviews-seed context:dev
-- comment: Тестові відгуки. Відгуки можуть залишати тільки юзери з DELIVERED+PAID замовленнями.
-- Поточні кваліфіковані замовлення: Order 1 (user 7, product 5)
-- Для повноцінного тестування додаємо додаткові доставлені замовлення

-- Додаткові доставлені замовлення для можливості залишити відгуки
INSERT INTO orders (
    id, purchase_type, session_id, user_id, store_id,
    buyer_name, buyer_surname, buyer_phone, buyer_email,
    delivery_type, carrier_id, delivery_address, tracking_number, payment_method,
    subtotal, tax, discount, total_amount, payment_status, shipping_status
) VALUES
-- User 8 купив iPhone 15 Pro (product 1) — доставлено
(8, 'REGULAR', NULL, 8, 1, 'Олена', 'П', '+380671234567', 'olena@ukr.net', 'BRANCH', 1, '{"city": "Дніпро", "branch": "№2"}', '59000111111', 'ONLINE_CARD', 52000.00, 0.00, 0.00, 52000.00, 'PAID', 'DELIVERED'),
-- User 9 купив Кава Ефіопія (product 10) — доставлено
(9, 'REGULAR', NULL, 9, 5, 'Сергій', 'Тех', '+380631112233', 'sergiy@outlook.com', 'BRANCH', 1, '{"city": "Київ", "branch": "№8"}', '59000222222', 'CASH_ON_DELIVERY', 950.00, 0.00, 0.00, 950.00, 'PAID', 'DELIVERED'),
-- User 10 купив MacBook Pro (product 2) — доставлено
(10, 'REGULAR', NULL, 10, 1, 'Марина', 'Шоп', '+380509998877', 'marina@test.com', 'COURIER', 1, '{"city": "Харків", "street": "Сумська, 15"}', '59000333333', 'ONLINE_CARD', 85000.00, 0.00, 0.00, 85000.00, 'PAID', 'DELIVERED'),
-- User 13 купив Дюна (product 8) — доставлено
(11, 'REGULAR', NULL, 13, 4, 'Дмитро', 'Дев', '+380998887766', 'dmytro@dev.ua', 'BRANCH', 2, '{"city": "Одеса", "branch": "№3"}', '59000444444', 'CASH_ON_DELIVERY', 650.00, 0.00, 0.00, 650.00, 'PAID', 'DELIVERED'),
-- User 14 купив Nike Air Force 1 (product 5) — доставлено
(12, 'REGULAR', NULL, 14, 3, 'Катерина', 'Арт', '+380507776655', 'katya@art.com', 'BRANCH', 1, '{"city": "Львів", "branch": "№7"}', '59000555555', 'ONLINE_CARD', 4500.00, 0.00, 0.00, 4500.00, 'PAID', 'DELIVERED');

INSERT INTO order_items (id, order_id, product_id, quantity, price_snapshot, title_snapshot, image_snapshot) VALUES
(8, 8, 1, 1, 52000.00, 'iPhone 15 Pro 256GB', 'iphone15pro.jpg'),
(9, 9, 10, 1, 950.00, 'Кава Ефіопія 1кг', 'coffee_eth.jpg'),
(10, 10, 2, 1, 85000.00, 'MacBook Pro M3 14"', 'mbp.jpg'),
(11, 11, 8, 1, 650.00, 'Дюна. Френк Герберт', 'dune.jpg'),
(12, 12, 5, 1, 4500.00, 'Nike Air Force 1', 'nike_af1.jpg');

INSERT INTO transaction_events (id, idempotency_key, order_id, event_type, amount) VALUES
(7, 'liqpay-2001', 8, 'PAYMENT', 52000.00),
(8, 'liqpay-2003', 10, 'PAYMENT', 85000.00),
(9, 'liqpay-2005', 12, 'PAYMENT', 4500.00);

-- Тепер додаємо відгуки від цих юзерів
INSERT INTO product_reviews (id, product_id, user_id, rating, comment) VALUES
-- User 7 на Nike Air Force 1 (Order 1, DELIVERED)
(1, 5, 7, 5, 'Відмінні кросівки! Дуже зручні та стильні. Рекомендую всім.'),
-- User 8 на iPhone 15 Pro (Order 8, DELIVERED)
(2, 1, 8, 4, 'Чудовий телефон, камера вражає. Єдиний мінус — ціна.'),
-- User 9 на Кава Ефіопія (Order 9, DELIVERED)
(3, 10, 9, 5, 'Найкраща кава що я пробував! Аромат неймовірний, буду замовляти ще.'),
-- User 10 на MacBook Pro (Order 10, DELIVERED)
(4, 2, 10, 5, 'MacBook Pro M3 — звір! Працює блискавично, екран казковий.'),
-- User 13 на Дюна (Order 11, DELIVERED)
(5, 8, 13, 3, 'Книга непогана, але переклад міг бути кращим. Обкладинка гарна.'),
-- User 14 на Nike Air Force 1 (Order 12, DELIVERED) — другий відгук на той самий товар
(6, 5, 14, 4, 'Класичні кросівки, виглядають круто. Трохи натирають перші дні.');

-- Оновлюємо rating та reviews_count в таблиці products
-- Product 5 (Nike Air Force 1): 2 відгуки, середній рейтинг = (5+4)/2 = 4.50
UPDATE products SET rating = 4.50, reviews_count = 2 WHERE id = 5;
-- Product 1 (iPhone 15 Pro): 1 відгук, рейтинг = 4.00
UPDATE products SET rating = 4.00, reviews_count = 1 WHERE id = 1;
-- Product 10 (Кава Ефіопія): 1 відгук, рейтинг = 5.00
UPDATE products SET rating = 5.00, reviews_count = 1 WHERE id = 10;
-- Product 2 (MacBook Pro): 1 відгук, рейтинг = 5.00
UPDATE products SET rating = 5.00, reviews_count = 1 WHERE id = 2;
-- Product 8 (Дюна): 1 відгук, рейтинг = 3.00
UPDATE products SET rating = 3.00, reviews_count = 1 WHERE id = 8;

-- Синхронізація сіквенсів для нових таблиць та модифікованих
SELECT setval('product_reviews_id_seq', (SELECT COALESCE(MAX(id), 1) FROM product_reviews));
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 1) FROM orders));
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 1) FROM order_items));
SELECT setval('transaction_events_id_seq', (SELECT COALESCE(MAX(id), 1) FROM transaction_events));

-- rollback DELETE FROM product_reviews; UPDATE products SET rating = 0.00, reviews_count = 0; DELETE FROM transaction_events WHERE id IN (7,8,9); DELETE FROM order_items WHERE id IN (8,9,10,11,12); DELETE FROM orders WHERE id IN (8,9,10,11,12);
