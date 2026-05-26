-- liquibase formatted sql

-- changeset andrii:15-user-settings-seed context:dev
INSERT INTO user_settings (user_id, notify_email_on_order_status) VALUES 
(1, TRUE),
(2, FALSE),
(7, TRUE),
(8, FALSE),
(9, TRUE);
-- rollback TRUNCATE TABLE user_settings;