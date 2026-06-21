# Data Dictionary: Sellora

**Версія:** 3.0
**Дата оновлення:** Червень 2026
**Статус:** Approved / Production-Ready
**Зміни архітектури:** Повністю синхронізовано з DBML-схемою. Додано сутності `user_settings`, `user_favorites`, `promo_codes`, `promo_usage_history`, `shipping_carriers`, `store_shipping_methods` та `product_reviews`. Розширено фінансову та логістичну деталізацію замовлень.

---

### 1. Сутність: `users` (Користувачі)

**Опис:** Глобальний реєстр акаунтів платформи. Відповідає за ідентифікацію, авторизацію та рольову модель.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор користувача | `1001` |
| `username` | VARCHAR(255) | Unique, Not Null | Унікальний нікнейм користувача | `andrij_dev` |
| `email` | VARCHAR(255) | Unique, Not Null | Електронна пошта (логін) | `user@sellora.com` |
| `password_hash` | VARCHAR(255) | Not Null | Хеш пароля (BCrypt) | `$2a$10$wYp...` |
| `role` | VARCHAR(50) | Not Null | Роль користувача в системі | `BUYER` |
| `avatar_url` | VARCHAR(500) | Nullable | Посилання на аватар | `https://.../me.jpg` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Дата та час створення акаунта | `2026-06-01 12:00:00+00` |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Дата та час останнього оновлення | `2026-06-01 12:00:00+00` |

**Обмеження (Checks & Keys):**

* `uq_users_email`: `UNIQUE (email)`
* `uq_users_username`: `UNIQUE (username)`
* `chk_users_role`: `CHECK (role IN ('BUYER', 'MERCHANT', 'ADMIN'))`

---

### 2. Сутність: `user_settings` (Налаштування користувача)

**Опис:** Профіль індивідуальних системних налаштувань. Зв'язок 1:1 з `users`.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `user_id` | BIGINT | PK, FK, Not Null | Власник налаштувань (збігається з `users.id`) | `1001` |
| `notify_email_on_order_status` | BOOLEAN | Not Null, Default: `true` | Дозвіл на email-сповіщення про замовлення | `true` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Час створення профілю налаштувань | `2026-06-01 12:00:00+00` |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Час оновлення налаштувань | `2026-06-01 12:00:00+00` |

**Схема поведінки (FK Strategy):**

* `user_id`: `ON DELETE CASCADE`

---

### 3. Сутність: `user_favorites` (Обрані товари / Wishlist)

**Опис:** Зв'язкова таблиця для зберігання товарів, які користувач додав у закладки.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `product_id` | BIGINT | PK (1/2), FK, Not Null | ID обраного товару | `500` |
| `user_id` | BIGINT | PK (2/2), FK, Not Null | ID користувача | `1001` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Дата додавання в обране | `2026-06-01 15:30:00+00` |

**Індекси та зв'язки:**

* **Composite PK:** `(user_id, product_id)`
* `idx_user_favorites_product_id`: `BTREE (product_id)`
* `user_id`: `ON DELETE CASCADE`
* `product_id`: `ON DELETE CASCADE`

---

### 4. Сутність: `stores` (Вітрини магазинів)

**Опис:** Публічний профіль магазину мерчанта. Один продавець — одна вітрина (1:1).

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор вітрини | `10` |
| `owner_id` | BIGINT | Unique, FK, Not Null | Власник вітрини (Мерчант) | `1002` |
| `name` | VARCHAR(255) | Unique, Not Null | Публічна назва магазину | `Tech Store UA` |
| `slug` | VARCHAR(255) | Unique, Not Null | ЧПУ-ідентифікатор для URL | `tech-store-ua` |
| `address` | TEXT | Nullable | Фізична адреса для самовивозу/повернень | `м. Київ, вул. Хрещатик 1` |
| `contact_phone` | VARCHAR(20) | Nullable | Номер служби підтримки магазину | `+380441112233` |
| `description` | TEXT | Nullable | Опис діяльності та товарів | `Офіційний дилер...` |
| `logo_url` | VARCHAR(500) | Nullable | Логотип бренду | `https://.../logo.png` |
| `rating` | DECIMAL(3,2) | Default: `0.00` | Середній рейтинг вітрини | `4.95` |
| `status` | VARCHAR(20) | Default: `'PENDING'` | Поточний бізнес-статус вітрини | `ACTIVE` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `chk_stores_status`: `CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'CLOSED'))`
* `chk_stores_rating`: `CHECK (rating >= 0.00 AND rating <= 5.00)`
* `idx_stores_slug`: `BTREE (slug)`
* `owner_id`: `ON DELETE CASCADE`

---

### 5. Сутність: `merchant_requisites` (Фінансові реквізити)

**Опис:** Розрахункові банківські рахунки мерчанта для виплати коштів з LiqPay.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор реквізитів | `5` |
| `owner_id` | BIGINT | FK, Not Null | Посилання на акаунт Мерчанта | `1002` |
| `edrpou` | VARCHAR(10) | Unique, Not Null | Код ЄДРПОУ або ІПН ФОПа | `3123456789` |
| `iban` | VARCHAR(29) | Not Null | Банківський рахунок у форматі IBAN | `UA893052990000026001123456789` |
| `bank_name` | VARCHAR(255) | Not Null | Офіційна назва банку отримувача | `АТ «УНІВЕРСАЛ БАНК»` |
| `is_primary` | BOOLEAN | Default: `false` | Прапорець основного рахунку для виплат | `true` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `chk_merchant_iban_format`: `CHECK (iban LIKE 'UA%' AND char_length(iban) = 29)`
* `uq_merchant_primary_iban`: `UNIQUE BTREE (owner_id) WHERE is_primary = TRUE` (гарантія 1 основного IBAN)
* `owner_id`: `ON DELETE CASCADE`

---

### 6. Сутність: `carts` (Кошики користувачів)

**Опис:** Заголовок активного кошика покупця. Зв'язок 1:1.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор кошика | `100` |
| `user_id` | BIGINT | Unique, FK, Not Null | Власник кошика | `1001` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та зв'язки:**

* `uq_carts_user`: `UNIQUE (user_id)`
* `user_id`: `ON DELETE CASCADE`

---

### 7. Сутність: `cart_items` (Товари в кошику)

**Опис:** Рядки наповнення кошика з підтримкою кількості.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор позиції в кошику | `250` |
| `product_id` | BIGINT | FK, Not Null | Посилання на товар | `500` |
| `cart_id` | BIGINT | FK, Not Null | Посилання на батьківський кошик | `100` |
| `quantity` | INT | Not Null | Кількість одиниць товару | `2` |
| `added_at` | TIMESTAMPTZ | Default: `now()` | Час додавання товару в кошик | `2026-06-01 16:00:00+00` |

**Обмеження та індекси:**

* `uq_cart_items_product`: `UNIQUE (cart_id, product_id)` (захист від дублікатів рядків)
* `chk_cart_items_quantity`: `CHECK (quantity > 0)`
* `idx_cart_items_product_id`: `BTREE (product_id)`
* `cart_id`: `ON DELETE CASCADE`
* `product_id`: `ON DELETE CASCADE`

---

### 8. Сутність: `categories` (Категорії каталогу)

**Опис:** Ієрархічний класифікатор товарів (Adjacency List).

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор категорії | `2` |
| `name` | VARCHAR(100) | Unique, Not Null | Назва категорії | `Смартфони` |
| `parent_id` | BIGINT | FK, Nullable | Посилання на батьківську категорію | `1` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `uq_categories_name`: `UNIQUE (name)`
* `idx_categories_parent_id`: `BTREE (parent_id)`
* `parent_id`: `ON DELETE RESTRICT` (заборона видалення категорії, якщо в ній є підкатегорії)

---

### 9. Сутність: `products` (Каталог товарів)

**Опис:** Основна товарна одиниця (SKU) з підтримкою подвійних цін (Dual Pricing).

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор товару | `500` |
| `store_id` | BIGINT | FK, Not Null | Вітрина приналежності | `10` |
| `category_id` | BIGINT | FK, Not Null | Категорія розміщення | `2` |
| `title` | VARCHAR(255) | Not Null | Коротке найменування | `iPhone 15 Pro 256GB` |
| `description` | TEXT | Nullable | Повний HTML/Markdown опис | `Титановий корпус...` |
| `attributes` | JSONB | GIN Index | Словник динамічних характеристик | `{"color": "black", "ram": "8GB"}` |
| `images` | JSONB | Nullable | Масив посилань на Cloudinary | `["https://.../1.jpg"]` |
| `stock_quantity` | INTEGER | Not Null | Фізичний залишок на складі | `15` |
| `standard_price` | DECIMAL(10,2) | Not Null | Роздрібна ціна (Regular) | `52000.00` |
| `group_price` | DECIMAL(10,2) | Nullable | Акційна ціна для спільної покупки | `48000.00` |
| `group_target_size` | INTEGER | Nullable | Цільова кількість людей у групі | `3` |
| `status` | VARCHAR(50) | Not Null | Стан відображення | `ACTIVE` |
| `rating` | DECIMAL(3,2) | Not Null, Default: `0.0` | Динамічний рейтинг товару | `4.85` |
| `reviews_count` | INT | Not Null, Default: `0` | Кешований лічильник відгуків | `12` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `chk_products_stock`: `CHECK (stock_quantity >= 0)`
* `chk_products_standard_price`: `CHECK (standard_price >= 0)`
* `chk_products_status`: `CHECK (status IN ('ACTIVE', 'OUT_OF_STOCK', 'ARCHIVED'))`
* `chk_products_group_sale`: `CHECK ((group_price IS NULL AND group_target_size IS NULL) OR (group_price >= 0 AND group_target_size > 1))`
* `idx_products_store_id`: `BTREE (store_id)`
* `idx_products_category_id`: `BTREE (category_id)`
* `idx_products_attributes`: `GIN (attributes)` (забезпечує миттєвий фасетний пошук)
* Зв'язки `store_id` та `category_id`: `ON DELETE RESTRICT`

---

### 10. Сутність: `group_buy_sessions` (Сесії спільних покупок)

**Опис:** Активна або завершена кімната спільної покупки.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор сесії | `9001` |
| `uuid` | VARCHAR(36) | Unique | Публічний UUID для шерінгу посилання | `a1b2c3d4-...` |
| `product_id` | BIGINT | FK, Not Null | Товар, що викуповується | `500` |
| `initiator_id` | BIGINT | FK, Not Null | Користувач, який відкрив сесію | `1001` |
| `status` | VARCHAR(50) | Not Null | Поточний стан кімнати | `ACTIVE` |
| `locked_price` | DECIMAL(10,2) | Not Null | Зафіксована ціна за одиницю | `48000.00` |
| `locked_target_size` | INTEGER | Not Null | Необхідна кількість покупців | `3` |
| `expires_at` | TIMESTAMPTZ | Not Null | Точний дедлайн згоряння сесії | `2026-06-02 16:05:00+00` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `uq_group_sessions_uuid`: `UNIQUE (uuid)`
* `chk_group_sessions_status`: `CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))`
* `idx_group_sessions_product_id`: `BTREE (product_id)`
* `idx_group_sessions_initiator_id`: `BTREE (initiator_id)`
* `idx_group_sessions_active_expires`: `BTREE (expires_at) WHERE status = 'ACTIVE'` (оптимізація для Schedulers)
* `product_id`: `ON DELETE RESTRICT`, `initiator_id`: `ON DELETE CASCADE`

---

### 11. Сутність: `group_members` (Учасники сесії)

**Опис:** Лог приєднання покупців до кімнат Group Buy.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `15000` |
| `session_id` | BIGINT | FK, Not Null | Посилання на кімнату | `9001` |
| `user_id` | BIGINT | FK, Not Null | Посилання на учасника | `1003` |
| `joined_at` | TIMESTAMPTZ | Default: `now()` | Час вступу в кімнату | `2026-06-01 16:10:00+00` |

**Обмеження та індекси:**

* `uq_group_members_session_user`: `UNIQUE (session_id, user_id)` (захист від повторного вступу)
* `idx_group_members_session_id`: `BTREE (session_id)`
* `idx_group_members_user_id`: `BTREE (user_id)`
* Обидва зовнішні ключі: `ON DELETE CASCADE`

---

### 12. Сутність: `promo_codes` (Промокоди)

**Опис:** Маркетинговий інструмент знижок.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор купона | `77` |
| `code` | VARCHAR(255) | Unique, Not Null | Текст купона | `SUMMER2026` |
| `discount_type` | VARCHAR(50) | Not Null | Тип: `'PERCENTAGE'` або `'FIXED'` | `PERCENTAGE` |
| `value` | DECIMAL(10,2) | Not Null | Розмір знижки (відсотки або гривні) | `10.00` |
| `start_date` | TIMESTAMPTZ | Not Null | Початок дії промокоду | `2026-06-01 00:00:00+00` |
| `end_date` | TIMESTAMPTZ | Not Null | Дедлайн дії промокоду | `2026-06-30 23:59:59+00` |
| `usage_limit` | INT | Not Null | Максимальна кількість використань | `100` |
| `used_count` | INT | Not Null, Default: `0` | Скільки разів купон вже активовано | `14` |
| `is_active` | BOOLEAN | Not Null, Default: `true` | Ручний вимикач купона | `true` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `uq_promo_codes_code`: `UNIQUE (code)`
* `chk_promo_codes_discount_type`: `CHECK (discount_type IN ('PERCENTAGE', 'FIXED'))`
* `chk_promo_codes_value`: `CHECK (value > 0 AND (discount_type = 'FIXED' OR (discount_type = 'PERCENTAGE' AND value <= 100)))`
* `chk_promo_codes_dates`: `CHECK (start_date <= end_date)`
* `chk_promo_codes_used_count`: `CHECK (usage_limit >= used_count)`
* `idx_promo_codes_code`: `BTREE (code)`

---

### 13. Сутність: `promo_usage_history` (Історія використання промо)

**Опис:** Журнал фіксації застосованих купонів для контролю лімітів.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `100` |
| `promo_id` | BIGINT | FK, Not Null | Застосований купон | `77` |
| `user_id` | BIGINT | FK, Not Null | Хто застосував | `1001` |
| `order_id` | BIGINT | FK, Not Null | В якому замовленні | `7500` |
| `used_at` | TIMESTAMPTZ | Default: `now()` | Дата транзакції | `2026-06-01 16:05:00+00` |

**Обмеження та індекси:**

* `uq_promo_usage_history_usage`: `UNIQUE (promo_id, user_id, order_id)`
* `idx_promo_usage_history_promo_id`: `BTREE (promo_id)`
* `promo_id`: `ON DELETE CASCADE`, решта: `ON DELETE RESTRICT`

---

### 14. Сутність: `shipping_carriers` (Поштові оператори)

**Опис:** Довідник інтегрованих логістичних служб.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `1` |
| `name` | VARCHAR(100) | Not Null | Публічна назва | `Нова Пошта` |
| `code` | VARCHAR(50) | Unique, Not Null | Системний код для API | `NOVA_POSHTA` |
| `is_active` | BOOLEAN | Default: `true` | Чи підтримується платформою зараз | `true` |

* `uq_shipping_carriers_code`: `UNIQUE (code)`

---

### 15. Сутність: `store_shipping_methods` (Методи доставки магазину)

**Опис:** Які саме оператори активовані конкретним продавцем.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `store_id` | BIGINT | PK (1/2), FK, Not Null | ID вітрини | `10` |
| `carrier_id` | BIGINT | PK (2/2), FK, Not Null | ID поштового оператора | `1` |
| `is_enabled` | BOOLEAN | Default: `true` | Прапорець активності | `true` |

* **Composite PK:** `(store_id, carrier_id)`
* Обидва ключі: `ON DELETE CASCADE`

---

### 16. Сутність: `orders` (Замовлення - Header)

**Опис:** Головний документ угоди. Містить повну фінансову, контактну та логістичну інформацію.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | Номер замовлення | `7500` |
| `purchase_type` | VARCHAR(50) | Not Null | Механіка: `'REGULAR'` або `'GROUP_BUY'` | `GROUP_BUY` |
| `session_id` | BIGINT | FK, Nullable | Прив'язка до кімнати (якщо Group Buy) | `9001` |
| `user_id` | BIGINT | FK, Not Null | Акаунт покупця | `1001` |
| `store_id` | BIGINT | FK, Not Null | Магазин виконання | `10` |
| `buyer_name` | VARCHAR(255) | Not Null | Ім'я отримувача | `Андрій` |
| `buyer_surname` | VARCHAR(255) | Not Null | Прізвище отримувача | `Пастух` |
| `buyer_phone` | VARCHAR(20) | Not Null | Телефон для накладної | `+380501112233` |
| `buyer_email` | VARCHAR(255) | Not Null | Email для чеку | `user@sellora.com` |
| `delivery_type` | VARCHAR(255) | Not Null | Тип: `'BRANCH'`, `'COURIER'`, `'PICKUP'` | `BRANCH` |
| `carrier_id` | BIGINT | FK, Nullable | Поштова служба | `1` |
| `delivery_address` | JSONB | Nullable | Словник реквізитів доставки (Snapshot) | `{"city": "Київ", "warehouse": "№1"}` |
| `tracking_number` | VARCHAR(100) | Nullable | ТТН поштового оператора | `20450011223344` |
| `payment_method` | VARCHAR(50) | Not Null | Метод: `'ONLINE_CARD'` або `'CASH_ON_DELIVERY'` | `ONLINE_CARD` |
| `order_comment` | TEXT | Nullable | Коментар покупця | `Не дзвонити` |
| `subtotal` | DECIMAL(10,2) | Not Null | Чиста вартість товарів | `48000.00` |
| `tax` | DECIMAL(10,2) | Default: `0.00` | 1% комісія маркетплейсу | `480.00` |
| `discount` | DECIMAL(10,2) | Default: `0.00` | Знижка за промокодом | `0.00` |
| `total_amount` | DECIMAL(10,2) | Not Null | Фінальна сума до сплати в LiqPay | `48480.00` |
| `payment_status` | VARCHAR(50) | Not Null | Фінансовий стан | `WAITING_FOR_GROUP` |
| `shipping_status` | VARCHAR(50) | Not Null | Логістичний стан | `PENDING` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `chk_orders_purchase_type`: `CHECK (purchase_type IN ('REGULAR', 'GROUP_BUY'))`
* `chk_orders_payment_method`: `CHECK (payment_method IN ('ONLINE_CARD', 'CASH_ON_DELIVERY'))`
* `chk_orders_payment_status`: `CHECK (payment_status IN ('WAITING_FOR_GROUP', 'PENDING', 'PAID', 'CANCELLED', 'REFUNDED'))`
* `chk_orders_shipping_status`: `CHECK (shipping_status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'))`
* `chk_orders_delivery_type`: `CHECK (delivery_type IN ('BRANCH', 'COURIER', 'PICKUP'))`
* `chk_orders_session_integrity`: `CHECK (purchase_type = 'REGULAR' OR (purchase_type = 'GROUP_BUY' AND session_id IS NOT NULL))`
* `chk_orders_financials_positive`: `CHECK (subtotal >= 0 AND tax >= 0 AND discount >= 0 AND total_amount >= 0)`
* Індекси `BTREE` по полях: `user_id`, `store_id`, `session_id`, `carrier_id`.
* **Стратегія FK:** Всі зовнішні ключі налаштовані на `ON DELETE RESTRICT` (або `SET NULL` для `session_id`) для повної заборони Hard Delete.

---

### 17. Сутність: `order_items` (Специфікація замовлення)

**Опис:** Рядки чеку зі Snapshot-ізоляцією цін та назв.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `1200` |
| `order_id` | BIGINT | FK, Not Null | Посилання на документ | `7500` |
| `product_id` | BIGINT | FK, Not Null | Посилання на товар | `500` |
| `quantity` | INT | Not Null | Кількість | `1` |
| `price_snapshot` | DECIMAL(10,2) | Not Null | Зафіксована ціна продажу | `48000.00` |
| `title_snapshot` | VARCHAR(255) | Not Null | Зафіксована назва | `iPhone 15 Pro` |
| `image_snapshot` | VARCHAR(1024) | Nullable | Зафіксований URL картинки | `https://...` |

* `chk_order_items_quantity`: `CHECK (quantity > 0)`
* `idx_order_items_product_id`: `BTREE (product_id)`
* `order_id`: `ON DELETE CASCADE`, `product_id`: `ON DELETE RESTRICT`

---

### 18. Сутність: `transaction_events` (Лог транзакцій LiqPay)

**Опис:** Незмінний (Append-Only) аудит платіжних подій.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `99999` |
| `idempotency_key` | VARCHAR(255) | Unique, Not Null | Унікальний ID транзакції з LiqPay | `liqpay_tx_12345` |
| `order_id` | BIGINT | FK, Not Null | Оплачене замовлення | `7500` |
| `event_type` | VARCHAR(50) | Not Null | Тип події | `PAYMENT` |
| `amount` | DECIMAL(10,2) | Not Null | Сума списання/повернення | `48480.00` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `uq_transactions_idempotency`: `UNIQUE (idempotency_key)`
* `chk_transactions_event_type`: `CHECK (event_type IN ('PAYMENT', 'FAILED', 'REFUND'))`
* `chk_transactions_amount`: `CHECK (amount > 0)`
* `idx_transactions_order_id`: `BTREE (order_id)`
* `order_id`: `ON DELETE RESTRICT`

---

### 19. Сутність: `product_reviews` (Відгуки про товари)

**Опис:** Користувацький рейтинг та коментарі до придбаних товарів.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| --- | --- | --- | --- | --- |
| `id` | BIGSERIAL | PK, Not Null | - | `300` |
| `product_id` | BIGINT | FK, Not Null | Оцінений товар | `500` |
| `user_id` | BIGINT | FK, Not Null | Автор відгуку | `1001` |
| `rating` | INT | Not Null | Оцінка від 1 до 5 | `5` |
| `comment` | TEXT | Nullable | Текст відгуку | `Чудовий телефон!` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Обмеження та індекси:**

* `chk_product_reviews_rating`: `CHECK (rating >= 1 AND rating <= 5)`
* `uq_product_reviews_user_product`: `UNIQUE (user_id, product_id)` (Бізнес-правило: 1 юзер = 1 відгук на 1 товар)
* `idx_product_reviews_product_id`: `BTREE (product_id)`
* `idx_product_reviews_user_id`: `BTREE (user_id)`
* Обидва зовнішні ключі: `ON DELETE CASCADE`