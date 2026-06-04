package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.OrderItem;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.User;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.OrderItemRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.UserRepository; // ДОДАНО
import com.sellora.core.infrastructure.persistence.StoreRepository; // ДОДАНО
import com.sellora.core.presentation.dtos.CheckoutRequestDto;
import com.sellora.core.presentation.dtos.OrderItemDto;
import com.sellora.core.presentation.dtos.OrderResponseDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.EmptyCartException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
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
  private final UserRepository userRepository; // ІН'ЄКЦІЯ
  private final StoreRepository storeRepository; // ІН'ЄКЦІЯ

  @Transactional
  // ДОДАЛИ CheckoutRequestDto В АРГУМЕНТИ:
  public OrderResponseDto checkout(Long userId, CheckoutRequestDto requestDto) {
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

    // --- ОТРИМАННЯ ДАНИХ ПОКУПЦЯ ТА МАГАЗИНУ ---
    User buyer = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Покупця не знайдено"));

    // --- СТАЛО (Шукає по ID магазину) ---
    Long storeId = items.get(0).getProduct().getMerchantId(); // Це насправді store_id
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин продавця не знайдено"));

    // 1. Зберігаємо Order
    Order order = new Order();
    order.setUserId(userId);
    order.setMerchantId(store.getId());

    // Фінанси
    order.setSubtotal(totalAmount);
    order.setTax(BigDecimal.ZERO);
    order.setDiscount(BigDecimal.ZERO);
    order.setFinalPrice(totalAmount);

    // Статуси
    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING");
    order.setPurchaseType("REGULAR");

    // --- ДАНІ ПОКУПЦЯ (БЕРЕМО З REQUEST DTO, А НЕ З USER) ---
    order.setBuyerName(requestDto.getBuyerName());
    order.setBuyerSurname(requestDto.getBuyerSurname());
    order.setBuyerPhone(requestDto.getBuyerPhone());

    order.setDeliveryType(requestDto.getDeliveryType());
    order.setPaymentMethod(requestDto.getPaymentMethod());

    // Email можемо залишити з юзера, оскільки він там є (судячи з твого скріншоту таблиці users)
    order.setBuyerEmail(buyer.getEmail());

    Order savedOrder = orderRepository.save(order);

    // 2. Зберігаємо OrderItems
    // ... (весь код збереження OrderItems і DTO залишається без змін)
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

    cartItemRepository.deleteAll(items);

    List<OrderItemDto> itemDtos = orderItems.stream().map(oi -> new OrderItemDto(
      oi.getProductId(),
      oi.getTitleSnapshot(),
      oi.getImageSnapshot(),
      oi.getPriceSnapshot(),
      oi.getQuantity(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity()))
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
