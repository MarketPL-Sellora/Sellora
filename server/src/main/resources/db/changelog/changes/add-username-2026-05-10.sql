-- liquibase formatted sql

-- changeset andrii:14-add-username-column
-- Фаза 1: Додаємо колонку, ДОЗВОЛЯЮЧИ NULL
ALTER TABLE users 
ADD COLUMN username VARCHAR(255);

-- Фаза 2: Міграція даних (Backfill). Заповнюємо порожні поля для існуючих записів.
-- Щоб уникнути конфліктів унікальності, беремо частину email до '@' і додаємо ID користувача
UPDATE users 
SET username = split_part(email, '@', 1) || '_' || id 
WHERE username IS NULL;

-- Фаза 3: Накладаємо жорсткі обмеження (Constraints), коли NULL значень більше немає
ALTER TABLE users 
ALTER COLUMN username SET NOT NULL;

-- Додаємо перевірку на унікальність (якщо це вимагається бізнес-логікою)
ALTER TABLE users 
ADD CONSTRAINT uq_users_username UNIQUE (username);