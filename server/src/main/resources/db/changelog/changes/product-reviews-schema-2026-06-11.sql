-- liquibase formatted sql

-- ==========================================
-- DOMAIN: PRODUCT REVIEWS
-- ==========================================

-- changeset andrii:24-product-reviews
CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT uq_product_reviews_user_product UNIQUE (user_id, product_id),

    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_reviews_product_id ON product_reviews USING btree(product_id);
CREATE INDEX idx_product_reviews_user_id ON product_reviews USING btree(user_id);
-- rollback DROP TABLE product_reviews CASCADE;


-- changeset andrii:25-alter-products-add-reviews-fields
ALTER TABLE products
    ADD COLUMN rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN reviews_count INT NOT NULL DEFAULT 0;
-- rollback ALTER TABLE products DROP COLUMN rating, DROP COLUMN reviews_count;
