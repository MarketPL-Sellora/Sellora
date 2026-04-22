# Data Dictionary: Sellora

**Версія:** 2.0
**Дата оновлення:** 22.04.2026
**Статус:** Approved
**Зміни архітектури:** Впроваджено патерн Header-Detail для замовлень, розмежовано публічну вітрину (`stores`) та фінанси (`merchant_requisites`), додано `carts`.

---

### 1. Сутність: `users` (Користувачі)
**Опис:** Глобальний реєстр акаунтів. Відповідає виключно за аутентифікацію та базовий профіль.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Унікальний ідентифікатор | 1001 |
| `email` | VARCHAR(255) | Unique, Not Null | Логін (електронна пошта) | user@mail.com |
| `password_hash` | VARCHAR(255) | Not Null | Хеш пароля (BCrypt) | `$2a$10$wYp...` |
| `role` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (role IN ('BUYER', 'MERCHANT', 'ADMIN'))` | BUYER |
| `avatar_url` | VARCHAR(500) | Nullable | Посилання на фото профілю | `https://.../img.jpg` |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Час створення запису | 2026-04-07 12:00:00+00 |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Час останнього оновлення | 2026-04-07 12:00:00+00 |

---

### 2. Сутність: `stores` (Вітрини магазинів)
**Опис:** Публічна інформація про магазин. Ізольована від чутливих фінансових даних.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор магазину | 10 |
| `owner_id` | BIGINT | Unique, FK, Not Null | Зв'язок 1:1 з користувачем-продавцем | 1002 |
| `name` | VARCHAR(255) | Unique, Not Null | Назва вітрини | Apple Reseller |
| `slug` | VARCHAR(255) | Unique, Not Null | URL-ідентифікатор (B-Tree Index) | `apple-reseller` |
| `address` | TEXT | Nullable | Фізична або юридична адреса | м. Київ... |
| `contact_phone` | VARCHAR(20) | Nullable | Контактний телефон | `+380501234567` |
| `description` | TEXT | Nullable | Опис магазину | Офіційний дилер... |
| `logo_url` | VARCHAR(500) | Nullable | Логотип вітрини | `https://.../logo.png` |
| `rating` | DECIMAL(3,2) | Default: 0.00 | **Business Rule:** `CHECK (rating >= 0.00 AND rating <= 5.00)` | 4.80 |
| `status` | VARCHAR(20) | Default: 'PENDING' | **Business Rule:** `CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED', 'CLOSED'))` | ACTIVE |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Час створення | 2026-04-07 12:00:00+00 |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Час оновлення | 2026-04-07 12:00:00+00 |

**Схема поведінки (FK Strategy):**
* `owner_id`: `ON DELETE CASCADE`.

---

### 3. Сутність: `merchant_requisites` (Фінансові реквізити)
**Опис:** Ізольоване зберігання юридичних та банківських даних продавця (PCI-DSS/Security compliance).

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор реквізиту | 5 |
| `owner_id` | BIGINT | FK, Not Null | Посилання на власника (не на вітрину!) | 1002 |
| `edrpou` | VARCHAR(10) | Unique, Not Null | ЄДРПОУ/РНОКПП | 12345678 |
| `iban` | VARCHAR(29) | Not Null | **Business Rule:** `CHECK (iban LIKE 'UA%' AND char_length(iban) = 29)` | UA12345... |
| `bank_name` | VARCHAR(255) | Not Null | Назва банку | ПриватБанк |
| `is_primary` | BOOLEAN | Default: FALSE | Прапорець основного рахунку для виплат | TRUE |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Час додавання рахунку | 2026-04-07 12:00:00+00 |

**Ключі та індекси:**
* **Partial Unique Index:** `(owner_id) WHERE is_primary = TRUE` — гарантує на рівні БД, що активним може бути лише 1 рахунок продавця.

**Схема поведінки (FK Strategy):**
* `owner_id`: `ON DELETE CASCADE`.

---

### 4. Сутність: `carts` (Кошики)
**Опис:** Заголовок кошика користувача. Зв'язок 1:1.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор кошика | 1 |
| `user_id` | BIGINT | Unique, FK, Not Null | Власник кошика | 1005 |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Дата створення | 2026-04-07 12:00:00+00 |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Дата оновлення | 2026-04-07 12:00:00+00 |

**Схема поведінки (FK Strategy):**
* `user_id`: `ON DELETE CASCADE`.

---

### 5. Сутність: `cart_items` (Елементи кошика)
**Опис:** Деталізація наповнення кошика.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор рядка кошика | 25 |
| `cart_id` | BIGINT | FK, Not Null | Посилання на кошик | 1 |
| `product_id` | BIGINT | FK, Not Null | Посилання на товар (B-Tree Index) | 500 |
| `quantity` | INT | Not Null | **Business Rule:** `CHECK (quantity > 0)` | 2 |
| `added_at` | TIMESTAMPTZ | Default: `now()` | Час додавання товару | 2026-04-07 12:00:00+00 |

**Ключі та індекси:**
* **Unique Composite Index:** `(cart_id, product_id)` — запобігає дублюванню товарів у кошику.

**Схема поведінки (FK Strategy):**
* `cart_id`: `ON DELETE CASCADE`.
* `product_id`: `ON DELETE CASCADE`.

---

### 6. Сутність: `categories` (Категорії)

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор категорії | 5 |
| `name` | VARCHAR(100) | Unique, Not Null | Назва категорії | Смартфони |
| `parent_id` | BIGINT | FK, Nullable | Батьківська категорія (B-Tree Index) | 1 |
| `created_at` | TIMESTAMPTZ | Default: `now()` | Час створення | 2026-04-07 12:00:00+00 |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | Час оновлення | 2026-04-07 12:00:00+00 |

**Схема поведінки (FK Strategy):**
* `parent_id`: `ON DELETE RESTRICT`.

---

### 7. Сутність: `products` (Товари)
**Опис:** Ядро каталогу. Містить бізнес-правила ціноутворення та залишків.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор товару | 500 |
| `store_id` | BIGINT | FK, Not Null | Вітрина, де розміщено товар (B-Tree Index) | 10 |
| `category_id` | BIGINT | FK, Not Null | Категорія товару (B-Tree Index) | 5 |
| `title` | VARCHAR(255) | Not Null | Назва | iPhone 15 |
| `description` | TEXT | Nullable | Детальний опис | ... |
| `attributes` | JSONB | GIN Index | EAV-модель для фільтрів пошуку | `{"color": "black"}` |
| `images` | JSONB | Nullable | Масив URL зображень | `["url1"]` |
| `stock_quantity` | INTEGER | Not Null | **Business Rule:** `CHECK (stock_quantity >= 0)` | 50 |
| `standard_price` | DECIMAL(10,2) | Not Null | **Business Rule:** `CHECK (standard_price >= 0)` | 45000.00 |
| `group_price` | DECIMAL(10,2) | Not Null | **Business Rule:** `CHECK (group_price >= 0)` | 40000.00 |
| `group_target_size` | INTEGER | Not Null | **Business Rule:** `CHECK (group_target_size > 1)` | 3 |
| `status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (status IN ('ACTIVE', 'OUT_OF_STOCK', 'ARCHIEVED'))` | ACTIVE |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |
| `updated_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Схема поведінки (FK Strategy):**
* `store_id`: `ON DELETE RESTRICT` (захист від каскаду смерті).
* `category_id`: `ON DELETE RESTRICT`.

---

### 8. Сутність: `group_buy_sessions` (Сесії групової покупки)

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор сесії | 9001 |
| `product_id` | BIGINT | FK, Not Null | Посилання на товар (B-Tree Index) | 500 |
| `initiator_id` | BIGINT | FK, Not Null | Покупець-ініціатор (B-Tree Index) | 1005 |
| `status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))` | ACTIVE |
| `locked_price` | DECIMAL(10,2) | Not Null | Зафіксована ціна акції | 40000.00 |
| `locked_target_size` | INTEGER | Not Null | Зафіксована кількість учасників | 3 |
| `expires_at` | TIMESTAMPTZ | Not Null | Дедлайн сесії | 2026-04-08 12:00+00 |

**Ключі та індекси:**
* **Partial Index:** `(expires_at) WHERE status = 'ACTIVE'` — для миттєвого пошуку сесій, що згорають за таймером.

**Схема поведінки (FK Strategy):**
* `product_id`: `ON DELETE RESTRICT`.
* `initiator_id`: `ON DELETE CASCADE`.

---

### 9. Сутність: `group_members` (Учасники групи)

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | - | 15000 |
| `session_id` | BIGINT | FK, Not Null | Посилання на сесію (B-Tree Index) | 9001 |
| `user_id` | BIGINT | FK, Not Null | Посилання на учасника (B-Tree Index) | 1005 |
| `joined_at` | TIMESTAMPTZ | Default: `now()` | Час приєднання | 2026-04-07 12:15+00 |

**Ключі та індекси:**
* **Unique Composite Index:** `(session_id, user_id)` — блокує повторний вступ.

**Схема поведінки (FK Strategy):**
* `session_id`: `ON DELETE CASCADE`.
* `user_id`: `ON DELETE CASCADE`.

---

### 10. Сутність: `orders` (Замовлення - Header)
**Опис:** Метадані замовлення. Управляє статусами оплати та доставки для всього кошика.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор документа | 7500 |
| `purchase_type` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (purchase_type IN ('REGULAR', 'GROUP_BUY'))` | REGULAR |
| `session_id` | BIGINT | FK, Nullable | Групова сесія (якщо `GROUP_BUY`) (B-Tree Index) | NULL |
| `user_id` | BIGINT | FK, Not Null | Покупець (B-Tree Index) | 1005 |
| `store_id` | BIGINT | FK, Not Null | Магазин-продавець (B-Tree Index) | 10 |
| `total_amount` | DECIMAL(10,2) | Not Null | Загальна сума чеку | 45000.00 |
| `payment_status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'))` | PAID |
| `shipping_status` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED'))` | PENDING |

**Складні перевірки:**
* **Session Integrity:** `CHECK (purchase_type = 'REGULAR' OR (purchase_type = 'GROUP_BUY' AND session_id IS NOT NULL))`

**Схема поведінки (FK Strategy):**
* Всі зовнішні ключі налаштовані на `ON DELETE RESTRICT` (або `SET NULL` для `session_id`), щоб захистити фінансову історію від фізичного видалення.

---

### 11. Сутність: `order_items` (Замовлення - Detail)
**Опис:** Фізичне наповнення замовлення зі збереженням історичних даних товару на момент чекауту.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | - | 1200 |
| `order_id` | BIGINT | FK, Not Null | Прив'язка до документа (Header) | 7500 |
| `product_id` | BIGINT | FK, Not Null | Оригінальний товар (B-Tree Index) | 500 |
| `quantity` | INT | Not Null | **Business Rule:** `CHECK (quantity > 0)` | 1 |
| `price_snapshot` | DECIMAL(10,2) | Not Null | Ціна на момент фіксації замовлення | 45000.00 |
| `title_snapshot` | VARCHAR(255) | Not Null | Назва на момент фіксації | iPhone 15 |
| `image_snapshot` | VARCHAR(1024) | Nullable | URL зображення | `https://...` |

**Схема поведінки (FK Strategy):**
* `order_id`: `ON DELETE CASCADE`.
* `product_id`: `ON DELETE RESTRICT`.

---

### 12. Сутність: `transaction_events` (Транзакційні події)
**Опис:** Append-Only лог для роботи з платіжним шлюзом.

| Назва поля | Тип даних | Обмеження | Опис | Приклад |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK, Not Null | Ідентифікатор події | 99999 |
| `idempotency_key` | VARCHAR(255) | Unique, Not Null | Ключ вебхука (захист від подвоєння) | `mono-tx-84...` |
| `order_id` | BIGINT | FK, Not Null | Цільове замовлення (B-Tree Index) | 7500 |
| `event_type` | VARCHAR(50) | Not Null | **Business Rule:** `CHECK (event_type IN ('HOLD', 'CAPTURE', 'REFUND'))` | CAPTURE |
| `amount` | DECIMAL(10,2) | Not Null | **Business Rule:** `CHECK (amount > 0)` | 45000.00 |
| `created_at` | TIMESTAMPTZ | Default: `now()` | - | - |

**Схема поведінки (FK Strategy):**
* `order_id`: `ON DELETE RESTRICT`.