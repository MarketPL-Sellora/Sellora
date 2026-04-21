-- changeset andrii:8
-- 8. Таблиця кошиків (1:1 з користувачем)
CREATE TABLE carts (
                     id BIGSERIAL PRIMARY KEY,
                     user_id BIGINT NOT NULL,
                     created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
                     updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),

  -- Обмеження: один користувач — один активний кошик
                     CONSTRAINT uq_carts_user UNIQUE (user_id),
                     CONSTRAINT fk_carts_user
                       FOREIGN KEY (user_id)
                         REFERENCES users(id)
                         ON DELETE CASCADE
);

-- rollback DROP TABLE carts;

-- changeset andrii:9
-- 9. Таблиця елементів кошика (1:N з кошиком)
CREATE TABLE cart_items (
                          id BIGSERIAL PRIMARY KEY,
                          cart_id BIGINT NOT NULL,
                          product_id BIGINT NOT NULL,
                          quantity INT NOT NULL,
                          added_at TIMESTAMP WITH TIME ZONE DEFAULT now(),

  -- Бізнес-правила та цілісність
                          CONSTRAINT uq_cart_items_product UNIQUE (cart_id, product_id),
                          CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),

                          CONSTRAINT fk_cart_items_cart
                            FOREIGN KEY (cart_id)
                              REFERENCES carts(id)
                              ON DELETE CASCADE,

                          CONSTRAINT fk_cart_items_product
                            FOREIGN KEY (product_id)
                              REFERENCES products(id)
                              ON DELETE CASCADE
);

-- rollback DROP TABLE cart_items;
