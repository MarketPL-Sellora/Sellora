package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.OrderItem;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.OrderItemRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import com.sellora.core.presentation.exceptions.EmptyCartException;
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
  private final OrderItemRepository orderItemRepository; // ДОДАЛИ НОВИЙ РЕПОЗИТОРІЙ

  @Transactional
  public Order checkout(Long userId) {
    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new RuntimeException("Кошик не знайдено для користувача: " + userId));

    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
    if (items.isEmpty()) {
      throw new EmptyCartException("Кошик порожній. Немає чого замовляти.");
    }

    BigDecimal totalAmount = items.stream()
      .map(item -> item.getProduct().getStandardPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 1. СТВОРЮЄМО ГОЛОВНЕ ЗАМОВЛЕННЯ (Order)
    Order order = new Order();
    order.setUserId(userId);
    order.setFinalPrice(totalAmount);
    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING");
    order.setPurchaseType("REGULAR");

    // Беремо merchantId з першого товару (якщо кошик підтримує товари тільки з одного магазину)
    order.setMerchantId(items.get(0).getProduct().getMerchantId());

    // Зберігаємо в базу, щоб Postgres згенерував ID
    Order savedOrder = orderRepository.save(order);

    // 2. СТВОРЮЄМО ДЕТАЛІ ЗАМОВЛЕННЯ (OrderItems)
    List<OrderItem> orderItems = items.stream().map(cartItem -> {
      Product product = cartItem.getProduct();

      OrderItem orderItem = new OrderItem();
      orderItem.setOrderId(savedOrder.getId()); // Прив'язуємо до головного замовлення!
      orderItem.setProductId(product.getId());
      orderItem.setQuantity(cartItem.getQuantity());
      orderItem.setPriceSnapshot(product.getStandardPrice()); // Зберігаємо ціну на момент покупки
      orderItem.setTitleSnapshot(product.getTitle());
      orderItem.setImageSnapshot(product.getImages());
      return orderItem;
    }).toList();

    // Зберігаємо всі товари замовлення в базу одним махом
    orderItemRepository.saveAll(orderItems);

    // 3. Очищаємо кошик
    cartItemRepository.deleteAll(items);

    return savedOrder;
  }
}
