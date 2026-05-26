-- liquibase formatted sql

-- changeset andrii:16-user-favorites-schema
CREATE TABLE user_favorites (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_favorites PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_favorites_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_favorites_product_id ON user_favorites USING btree(product_id);
-- rollback DROP TABLE user_favorites CASCADE;