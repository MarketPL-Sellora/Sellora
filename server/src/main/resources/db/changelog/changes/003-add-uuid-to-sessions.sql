-- liquibase formatted sql
-- changeset vlkorolenko:add-uuid-to-sessions

ALTER TABLE group_buy_sessions ADD COLUMN uuid VARCHAR(36) UNIQUE;
