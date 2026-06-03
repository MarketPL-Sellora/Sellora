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
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.OrderItemDto;
import com.sellora.core.presentation.dtos.OrderResponseDto;
import com.sellora.core.presentation.exceptions.ConflictException;
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
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;

  @Transactional
  public OrderResponseDto checkout(Long userId) { // <--- Змінили тип повернення
    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new RuntimeException("Кошик не знайдено для користувача: " + userId));

    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
    if (items.isEmpty()) {
      throw new EmptyCartException("Кошик порожній. Немає чого замовляти.");
    }

    // --- Валідація та списання залишків ---
    for (CartItem cartItem : items) {
      Product product = cartItem.getProduct();

      if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
        throw new ConflictException(
          String.format("Товар '%s' наразі недоступний для покупки (Статус: %s)",
            product.getTitle(), product.getStatus())
        );
      }

      if (cartItem.getQuantity() > product.getStockQuantity()) {
        throw new ConflictException(
          String.format("Товар '%s' недоступний у кількості %d шт. Залишок на складі: %d шт.",
            product.getTitle(), cartItem.getQuantity(), product.getStockQuantity())
        );
      }
      int newStock = product.getStockQuantity() - cartItem.getQuantity();
      product.setStockQuantity(newStock);
      if (newStock == 0) {
        product.setStatus("OUT_OF_STOCK");
      }
      productRepository.save(product);
    }

    BigDecimal totalAmount = items.stream()
      .map(item -> item.getProduct().getStandardPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 1. Зберігаємо Order
    Order order = new Order();
    order.setUserId(userId);
    order.setFinalPrice(totalAmount);
    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING");
    order.setPurchaseType("REGULAR");
    order.setMerchantId(items.get(0).getProduct().getMerchantId());

    Order savedOrder = orderRepository.save(order);

    // 2. Зберігаємо OrderItems
    List<OrderItem> orderItems = items.stream().map(cartItem -> {
      Product product = cartItem.getProduct();
      OrderItem orderItem = new OrderItem();
      orderItem.setOrderId(savedOrder.getId());
      orderItem.setProductId(product.getId());
      orderItem.setQuantity(cartItem.getQuantity());
      orderItem.setPriceSnapshot(product.getStandardPrice());
      orderItem.setTitleSnapshot(product.getTitle());
      String mainImage = (product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;
      orderItem.setImageSnapshot(mainImage);
      return orderItem;
    }).toList();

    orderItemRepository.saveAll(orderItems);

    // 3. Очищаємо кошик
    cartItemRepository.deleteAll(items);

    // 4. --- ЗБИРАЄМО ПОВНУ DTO ДЛЯ ФРОНТЕНДУ ---
    List<OrderItemDto> itemDtos = orderItems.stream().map(oi -> new OrderItemDto(
      oi.getProductId(),
      oi.getTitleSnapshot(),
      oi.getImageSnapshot(),
      oi.getPriceSnapshot(),
      oi.getQuantity(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())) // subTotal
    )).toList();

    return new OrderResponseDto(
      savedOrder.getId(),
      savedOrder.getUserId(),
      savedOrder.getMerchantId(),
      savedOrder.getFinalPrice(),
      savedOrder.getPaymentStatus(),
      savedOrder.getShippingStatus(),
      savedOrder.getPurchaseType(),
      itemDtos
    );
  }
}
