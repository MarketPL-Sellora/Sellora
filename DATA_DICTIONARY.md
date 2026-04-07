# Data Dictionary: Sellora

**Версія:** 1.0
**Дата останнього оновлення:** 31.03.2026
**Статус:** Approved

---

### 1. Сутність: `users` (Користувачі)
**Опис:** Зберігає облікові дані та розмежовує права доступу користувачів (покупців та продавців) до системи.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор користувача | 1001 |
| `email` | VARCHAR(255) | Unique, Not Null | Логін (електронна пошта) | user@mail.com |
| `password_hash` | VARCHAR(255) | Not Null | Хеш пароля | `$2a$12$R9h...` |
| `role` | VARCHAR(50) | Not Null | Роль доступу. **Business Rule:** `CHECK (role IN ('BUYER', 'MERCHANT'))` | BUYER |
| `created_at` | TIMESTAMP | Default: `now()` | Час створення запису | 2026-04-07 12:00:00 |
| `updated_at` | TIMESTAMP | Default: `now()` | Час останнього оновлення запису | 2026-04-07 12:00:00 |

**Схема поведінки (FK Strategy):**
Зовнішні ключі відсутні.

---

### 2. Сутність: `categories` (Категорії)
**Опис:** Ієрархічний довідник для класифікації товарного асортименту каталогу.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор категорії | 5 |
| `name` | VARCHAR(100) | Unique, Not Null | Назва категорії товарів | Смартфони |
| `category_id` | BIGINT | FK, Nullable | Вказує на батьківську категорію для побудови підкатегорій | 1 |
| `created_at` | TIMESTAMP | Default: `now()` | Час створення запису | 2026-04-07 12:00:00 |
| `updated_at` | TIMESTAMP | Default: `now()` | Час останнього оновлення запису | 2026-04-07 12:00:00 |

**Схема поведінки (FK Strategy):**
* `category_id`: `ON DELETE RESTRICT` (заборонено видаляти батьківську категорію, якщо існують вкладені підкатегорії).

---

### 3. Сутність: `products` (Товари)
**Опис:** Містить деталі асортименту продавців, фіксує поточні залишки та акційні умови для механіки «Group Buy».

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор товару | 500 |
| `merchant_id` | BIGINT | FK, Not Null, Index | Посилання на продавця (власника) | 1001 |
| `category_id` | BIGINT | FK, Not Null | Посилання на категорію | 5 |
| `title` | VARCHAR(255) | Not Null | Назва товарної позиції | iPhone 15 Pro |
| `description` | TEXT | Nullable | Детальний текстовий опис | Характеристики... |
| `attributes` | JSONB | GIN Index | Гнучкі характеристики для фільтрів пошуку | `{"color": "black", "memory": 256}` |
| `images` | JSONB | Nullable | Масив URL-посилань на зображення | `["url1", "url2"]` |
| `stock_quantity` | INT | Not Null | Залишок на складі. **Business Rule:** `CHECK (stock_quantity >= 0)` | 50 |
| `standard_price` | DECIMAL(10,2) | Not Null | Звичайна вартість. **Business Rule:** `CHECK (standard_price >= 0)` | 45000.00 |
| `group_price` | DECIMAL(10,2) | Not Null | Акційна вартість для групи. **Business Rule:** `CHECK (group_price >= 0)` | 40000.00 |
| `group_target_size` | INT | Not Null | Необхідна кількість людей у групі. **Business Rule:** `CHECK (group_target_size > 1)` | 3 |
| `status` | VARCHAR(50) | Not Null | Стан. **Business Rule:** `CHECK (status IN ('active', 'out of stock', 'archieved'))` | active |
| `created_at` | TIMESTAMP | Default: `now()` | Дата додавання товару | 2026-04-07 12:00:00 |
| `updated_at` | TIMESTAMP | Default: `now()` | Дата останньої зміни | 2026-04-07 12:00:00 |

**Схема поведінки (FK Strategy):**
* `merchant_id`: `ON DELETE CASCADE` (видалення профілю продавця видаляє весь його асортимент).
* `category_id`: `ON DELETE RESTRICT` (заборонено видаляти категорію, якщо до неї прив'язані товари).

---

### 4. Сутність: `group_buy_sessions` (Сесії групової покупки)
**Опис:** Агрегує логіку 24-годинних кімнат закупівлі. Фіксує умови акції (snapshot) на момент її створення ініціатором.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор сесії | 9001 |
| `product_id` | BIGINT | FK, Not Null | Посилання на товар | 500 |
| `initiator_id` | BIGINT | FK, Not Null | Посилання на покупця-ініціатора | 1002 |
| `status` | VARCHAR(50) | Not Null | Стан групи. **Business Rule:** `CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))` | ACTIVE |
| `locked_price` | DECIMAL(10,2) | Not Null | Зафіксована ціна для поточних учасників (захист від зміни ціни продавцем) | 40000.00 |
| `locked_target_size` | INT | Not Null | Зафіксований необхідний розмір групи | 3 |
| `expires_at` | TIMESTAMP | Not Null | Дедлайн (24 год). **Index:** Partial index `WHERE status = 'ACTIVE'` | 2026-04-08 12:00:00 |
| `created_at` | TIMESTAMP | Default: `now()` | Час відкриття сесії | 2026-04-07 12:00:00 |
| `updated_at` | TIMESTAMP | Default: `now()` | Час останньої зміни статусу | 2026-04-07 12:00:00 |

**Схема поведінки (FK Strategy):**
* `product_id`: `ON DELETE CASCADE`.
* `initiator_id`: `ON DELETE CASCADE`.

---

### 5. Сутність: `group_members` (Учасники групи)
**Опис:** Сполучна таблиця (Join Table), яка фіксує факт приєднання конкретних користувачів до активних сесій.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор запису | 15000 |
| `session_id` | BIGINT | FK, Not Null | Посилання на кімнату закупівлі | 9001 |
| `user_id` | BIGINT | FK, Not Null | Посилання на покупця | 1003 |
| `joined_at` | TIMESTAMP | Default: `now()` | Час натискання кнопки «Приєднатися» | 2026-04-07 12:15:00 |

**Ключі та індекси:**
* **Unique Composite Index** `(session_id, user_id)`: Архітектурний захист (Fair Play Rule) для недопущення ситуації, коли один користувач займає кілька місць через стан гонки (race condition).

**Схема поведінки (FK Strategy):**
* `session_id`: `ON DELETE CASCADE`.
* `user_id`: `ON DELETE CASCADE`.

---

### 6. Сутність: `orders` (Замовлення)
**Опис:** Головний документ продажу (Order-driven Checkout). Зберігає фінальний стан угоди (Snapshotting) для захисту історії замовлень від змін у каталозі.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор замовлення | 7500 |
| `purchase_type` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (purchase_type IN ('REGULAR', 'GROUP_BUY'))` | GROUP_BUY |
| `session_id` | BIGINT | FK, Nullable | Посилання на групу. **Business Rule:** `CHECK (session_id IS NOT NULL якщо purchase_type = 'GROUP_BUY')` | 9001 |
| `user_id` | BIGINT | FK, Not Null | Посилання на покупця | 1002 |
| `merchant_id` | BIGINT | FK, Not Null | Посилання на продавця | 1001 |
| `product_id` | BIGINT | FK, Nullable | Посилання на існуючий товар | 500 |
| `product_title_snapshot`| VARCHAR(255) | Not Null | Назва товару на момент оформлення (захист історії) | iPhone 15 Pro |
| `product_image_snapshot`| VARCHAR(1024) | Nullable | URL зображення на момент оформлення | `https://.../img.png` |
| `final_price` | DECIMAL(10,2) | Not Null | Фактично сплачена вартість | 40000.00 |
| `payment_status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'))` | PENDING |
| `shipping_status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED'))` | PENDING |
| `created_at` | TIMESTAMP | Default: `now()` | Час генерації замовлення | 2026-04-07 12:00:00 |
| `updated_at` | TIMESTAMP | Default: `now()` | Час останньої зміни статусів | 2026-04-07 12:00:00 |

**Схема поведінки (FK Strategy):**
* `session_id`: `ON DELETE SET NULL` (при скасуванні/видаленні сесії, історія замовлення як сутності зберігається).
* `product_id`: `ON DELETE SET NULL` (товар можна видалити з каталогу, але ордер виживе завдяки Snapshot-полям).
* `user_id`: `ON DELETE RESTRICT` (блокує видалення покупця, який має фінансову історію).
* `merchant_id`: `ON DELETE RESTRICT` (блокує видалення продавця, який має продажі).

---

### 7. Сутність: `transaction_events` (Транзакційні події)
**Опис:** Незмінний фінансовий лог (Append-Only Ledger). Відповідає за фіксацію холдування та списань, ізолюючи бізнес-логіку грошей.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор транзакції | 99999 |
| `idempotency_key` | VARCHAR(255) | Unique, Not Null | Ключ вебхука від шлюзу (захист від подвійного списання) | `mono-tx-84...` |
| `user_id` | BIGINT | FK, Not Null | Користувач, з якого утримуються кошти | 1002 |
| `order_id` | BIGINT | FK, Not Null | Прив'язка до конкретного замовлення | 7500 |
| `event_type` | VARCHAR(50) | Not Null | Тип фінансової операції. **Business Rule:** `CHECK (event_type IN ('HOLD', 'CAPTURE', 'REFUND'))` | HOLD |
| `amount` | DECIMAL(10,2) | Not Null | Сума операції. **Business Rule:** `CHECK (amount > 0)` | 40000.00 |
| `created_at` | TIMESTAMP | Default: `now()` | Фактичний час відпрацювання транзакції | 2026-04-07 12:05:00 |

**Схема поведінки (FK Strategy):**
* `user_id`: `ON DELETE RESTRICT` (фінансову історію користувача видаляти категорично заборонено).
* `order_id`: `ON DELETE RESTRICT` (замовлення, у якого були рухи коштів, не підлягає видаленню).