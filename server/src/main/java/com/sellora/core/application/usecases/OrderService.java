package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;

  @Transactional
  public Order checkout(Long userId) {
    // 1. Знаходимо кошик користувача
    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new RuntimeException("Кошик не знайдено для користувача: " + userId));

    // 2. Дістаємо всі товари з цього кошика
    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
    if (items.isEmpty()) {
      throw new RuntimeException("Кошик порожній. Немає чого замовляти.");
    }

    // 3. Рахуємо загальну суму (ціна товару * кількість)
    BigDecimal totalAmount = items.stream()
      .map(item -> item.getProduct().getStandardPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 4. Створюємо фінальне замовлення
    Order order = new Order();
    order.setUserId(userId);
    order.setFinalPrice(totalAmount);

    // --- СТАТУСИ ЗГІДНО З БАЗОЮ ДАНИХ ---
    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING"); // Було "NEW", змінив на "PENDING"
    order.setPurchaseType("REGULAR");   // Було "STANDARD", змінив на "REGULAR"

    // Беремо дані з першого товару в кошику
    Product productFromCart = items.get(0).getProduct();
    order.setMerchantId(productFromCart.getMerchantId());
    order.setProductId(productFromCart.getId());
    order.setProductTitleSnapshot(productFromCart.getTitle());
    //order.setSessionId(1L);

    Order savedOrder = orderRepository.save(order);

    // 5. Очищаємо кошик
    cartItemRepository.deleteAll(items);

    return savedOrder;
  }
}
