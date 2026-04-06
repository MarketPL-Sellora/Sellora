# 📖 Sellora Code Style Guide

Цей документ визначає стандарти написання коду для всіх учасників команди. Дотримання цих правил є
обов'язковим для успішного проходження Code Review.

## 1. Галузеві стандарти

- **Frontend (JS/TS):** [Airbnb Style Guide](https://github.com/airbnb/javascript)
- **Backend (Java):** [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## 2. Naming Conventions (Правила іменування)

### 🔹 Backend (Java)

- **Packages:** `snake_case` (наприклад, `user_controller`).
- **Classes:** `PascalCase` (наприклад, `ProductService`).
- **Methods/Variables:** `camelCase`.
- **Constants:** `UPPER_SNAKE_CASE`.

### 🔸 Frontend (React)

- **Folders:** `kebab-case` (наприклад, `ui-components`).
- **Components (.tsx):** `PascalCase`. Назва файлу = назва компонента.
- **Utils/Hooks (.ts):** `camelCase`.
- **Styles:** `PascalCase.module.css`.

### 🗄 Database

- **Tables:** `snake_case`, множина (наприклад, `orders`).
- **Columns:** `snake_case`, однина (наприклад, `total_price`).
- **FK (Foreign Keys):** `суть_id` (наприклад, `user_id`).

## 3. Загальні правила для всіх

- **Boolean:** Префікси `is`, `has`, `can` (наприклад, `isActive`, `canEdit`).
- **Abbreviations:** Пишемо `GetUi` (PascalCase) або `get_ui` (snake_case). **Заборонено:** `GetUI`.
- **Заборонені назви:** Сувора заборона на `data`, `info`, `temp`. Файл `data.*` — це "смітник".
  Використовуйте конкретику.

## 4. Форматування та коментарі

- **Відступи:** 2 пробіли (налаштовано в `.editorconfig`).
- **Довжина рядка:** Максимум 100 символів.
- **Коментарі:** Обов'язково використовуйте `JSDoc` / `JavaDoc` для публічних методів.
