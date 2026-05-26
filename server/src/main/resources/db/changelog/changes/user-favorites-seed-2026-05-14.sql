-- liquibase formatted sql

-- changeset andrii:17-user-favorites-seed context:dev
INSERT INTO user_favorites (user_id, product_id) VALUES 
(7, 1),
(7, 3),
(8, 2),
(9, 4),
(10, 1),
(10, 5);
-- rollback TRUNCATE TABLE user_favorites;