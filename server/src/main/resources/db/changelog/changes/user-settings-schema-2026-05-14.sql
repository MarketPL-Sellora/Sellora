-- liquibase formatted sql

-- changeset andrii:14-user-settings-schema
CREATE TABLE user_settings (
    user_id BIGINT PRIMARY KEY,
    notify_email_on_order_status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- rollback DROP TABLE user_settings CASCADE;