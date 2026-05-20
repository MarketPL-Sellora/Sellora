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
import com.sellora.core.presentation.dtos.OrderDetailsResponseDto;
import com.sellora.core.presentation.dtos.OrderItemDto;
import com.sellora.core.presentation.dtos.OrderResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.EmptyCartException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  @Transactional
  public Order checkout(Long userId) {
    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new BadRequestException("Кошик не знайдено для користувача: " + userId));

    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
    if (items.isEmpty()) {
      throw new EmptyCartException("Кошик порожній. Немає чого замовляти.");
    }

    BigDecimal totalAmount = items.stream()
      .map(item -> item.getProduct().getStandardPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Order order = new Order();
    order.setUserId(userId);
    order.setFinalPrice(totalAmount);
    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING");
    order.setPurchaseType("REGULAR");
    order.setMerchantId(items.get(0).getProduct().getMerchantId());

    Order savedOrder = orderRepository.save(order);

    List<OrderItem> orderItems = items.stream().map(cartItem -> {
      Product product = cartItem.getProduct();

      OrderItem orderItem = new OrderItem();
      orderItem.setOrderId(savedOrder.getId());
      orderItem.setProductId(product.getId());
      orderItem.setQuantity(cartItem.getQuantity());
      orderItem.setPriceSnapshot(product.getStandardPrice());
      orderItem.setTitleSnapshot(product.getTitle());

      String mainImage = (product.getImages() != null && !product.getImages().isEmpty())
        ? product.getImages().get(0)
        : null;
      orderItem.setImageSnapshot(mainImage);
      return orderItem;
    }).toList();

    orderItemRepository.saveAll(orderItems);
    cartItemRepository.deleteAll(items);

    return savedOrder;
  }

  public Page<OrderResponseDto> getUserOrders(
    Long userId, String shippingStatus, String paymentStatus,
    int page, int size, String sortBy, String sortDir) {

    List<String> allowedSortFields = List.of("id", "createdAt", "finalPrice", "paymentStatus", "shippingStatus");
    if (!allowedSortFields.contains(sortBy)) {
      throw new BadRequestException("Недопустиме поле для сортування: " + sortBy);
    }

    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    String cleanShipping = (shippingStatus != null && !shippingStatus.isBlank()) ? shippingStatus.trim() : null;
    String cleanPayment = (paymentStatus != null && !paymentStatus.isBlank()) ? paymentStatus.trim() : null;

    Page<Order> orders = orderRepository.findUserOrders(userId, cleanShipping, cleanPayment, pageable);

    return orders.map(order -> new OrderResponseDto(
      order.getId(),
      order.getPurchaseType(),
      order.getMerchantId(),
      order.getFinalPrice(),
      order.getPaymentStatus(),
      order.getShippingStatus(),
      order.getCreatedAt()
    ));
  }

  public OrderDetailsResponseDto getOrderById(Long orderId, Long userId) {
    // 1. Шукаємо замовлення (якщо немає - 404)
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new com.sellora.core.presentation.exceptions.ResourceNotFoundException("Замовлення не знайдено"));

    // 2. БЕЗПЕКА: Перевіряємо, чи належить воно тому, хто запитує (якщо чуже - 403)
    if (!order.getUserId().equals(userId)) {
      throw new com.sellora.core.presentation.exceptions.ForbiddenException("Ви не маєте доступу до цього замовлення");
    }

    // 3. Дістаємо товари
    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

    // 4. Мапимо товари в DTO
    List<OrderItemDto> itemDtos = items.stream()
      .map(item -> new OrderItemDto(
        item.getProductId(),
        item.getTitleSnapshot(),
        item.getImageSnapshot(),
        item.getQuantity(),
        item.getPriceSnapshot()
      )).toList();

    // 5. Збираємо фінальну відповідь
    return new OrderDetailsResponseDto(
      order.getId(),
      order.getPurchaseType(),
      order.getMerchantId(),
      order.getFinalPrice(),
      order.getPaymentStatus(),
      order.getShippingStatus(),
      order.getCreatedAt(),
      itemDtos
    );
  }
}
