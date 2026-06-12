# 🛍️ Sellora Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg?logo=java)](https://www.oracle.com/java/)
[![Vue.js](https://img.shields.io/badge/Vue.js-Frontend-4FC08D.svg?logo=vuedotjs)](https://vuejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)]()

> **Sellora** — це сучасна багатокористувацька e-commerce платформа (маркетплейс), яка об'єднує функціонал класичного інтернет-магазину та унікальну механіку **групових покупок** (Group Buy).

---

## 🌟 Ключові можливості (Features)

### Для Покупців (Buyers)
* **Групові покупки:** Об'єднання з іншими користувачами для отримання оптової знижки.
* **Авторизація:** Надійна локальна реєстрація та управління сесіями через JWT.
* **Каталог та Відгуки:** Оптимізована фільтрація, захист від фейкових відгуків (відгук можливий лише після отримання товару: статуси `DELIVERED` та `PAID`).
* **Онлайн-оплата:** Безпечна інтеграція з платіжною системою **LiqPay**.

### Для Продавців (Merchants)
* **Store Onboarding:** Багатокроковий процес реєстрації магазину з автоматичною ескалацією ролей (RBAC) від BUYER до MERCHANT.
* **Управління:** Налаштування лімітів для групових покупок, управління залишками на складі, зміна статусів доставки.

---

## 📂 Структура проєкту

Репозиторій побудований за принципом монорепи та містить усі необхідні компоненти системи:

* `/client` — вихідний код клієнтської частини (Frontend на Vue.js).
* `/server` — вихідний код серверної частини (Backend на Spring Boot / Java 17).
* `/server/src/main/resources/db/changelog` — міграції бази даних (Liquibase).
* `docker-compose.yml` — конфігурація для одночасного підняття всієї інфраструктури (БД, pgAdmin, Backend, Frontend).

---

## 💻 Системні вимоги

Для успішного локального розгортання платформи ваш комп'ютер має відповідати наступним мінімальним вимогам:

* **OS:** Windows 10/11 (з WSL2), macOS, Linux
* **CPU:** Мінімум 2 ядра (Рекомендовано 4)
* **RAM:** Мінімум 4 GB вільної пам'яті (Рекомендовано 8 GB для комфортної роботи Docker)
* **Docker:** Версія 24.0+
* **Docker Compose:** Версія v2.20+ (вбудовано в Docker Desktop)
* **Git**

---

## 🚀 Встановлення та запуск (Getting Started)

Завдяки контейнеризації, проєкт можна запустити однією командою без необхідності локально встановлювати Java, Node.js чи PostgreSQL.

### 1. Клонування репозиторію

    git clone https://github.com/your-username/sellora.git
    cd sellora

### 2. Конфігурація середовища (.env)
У корені проєкту знаходиться файл `.env.example`.
1. Створіть його точну копію з назвою `.env`.
2. За необхідності (для тестування реальних оплат чи збереження файлів) заповніть ключі `LIQPAY_PUBLIC_KEY`, `LIQPAY_PRIVATE_KEY` та `CLOUDINARY_URL`. Паролі до бази даних можна залишити без змін для локальної розробки.

### 3. Запуск платформи (Docker Compose)
Виконайте команду в кореневій папці проєкту:

    docker-compose up --build -d

*Ця команда створить мережу, підніме PostgreSQL, запустить pgAdmin, збере Backend (Maven) та Frontend, і застосує міграції Liquibase автоматично під час старту Spring Boot.*

### 4. Точки доступу (Endpoints)
Після успішного запуску (може зайняти кілька хвилин при першому білді) система буде доступна за адресами:
* **Frontend (Клієнт):** http://localhost:80
* **Backend API:** http://localhost:8080/api/v1
* **Swagger UI (Документація API):** http://localhost:8080/swagger-ui.html
* **pgAdmin (Управління БД):** http://localhost:5050

---

## 🔐 Доступи для тестування (Credentials)

Для швидкого тестування функціоналу ви можете використати наступні попередньо створені акаунти (генеруються автоматично через міграції):

**Адміністратор (Admin):**
* Email: `admin@sellora.com`
* Пароль: `password123`

**Продавець (Merchant):**
* Email: `merchant@sellora.com`
* Пароль: `password123`

**Покупець (Buyer):**
* Email: `buyer@sellora.com`
* Пароль: `password123`

**Доступ до бази даних (pgAdmin):**
* Email: `admin@admin.com` (або той, що вказаний у вашому `.env` як `PGADMIN_EMAIL`)
* Пароль: `admin` (або `PGADMIN_PASSWORD`)

---

## ⚙️ Технічні деталі бекенду

* **Стратегія міграції:** Використовується **Liquibase** (`db.changelog-master.yaml`). Усі зміни схеми накочуються автоматично при старті бекенд-контейнера.
* **Безпека:** RBAC налаштований через Spring Security (`@PreAuthorize`). Валідація токенів відбувається через кастомний JWT-фільтр.
* **Фонові процеси:** CRON-задачі (`@EnableScheduling`) автоматично скасують незібрані групові покупки та повертають товари на склад.
* **Оптимізація:** Проблема N+1 (наприклад, при завантаженні товарів та їх відгуків) вирішена через нативні JPQL Constructor Expressions.
