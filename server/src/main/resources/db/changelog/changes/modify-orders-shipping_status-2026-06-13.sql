-- liquibase formatted sql

-- changeset andrii:27-add-cancelled-to-shipping-status
-- Опис: Розширення списку статусів доставки. Додавання статусу 'CANCELLED' через перевизначення CHECK констрайнту.

-- Створюємо новий констрайнт з доданим статусом 'CANCELLED'
ALTER TABLE orders ADD CONSTRAINT chk_orders_shipping_status 
    CHECK (shipping_status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- rollback ALTER TABLE orders DROP CONSTRAINT chk_orders_shipping_status;