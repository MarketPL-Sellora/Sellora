package com.sellora.core.application.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.domain.entities.*;
import com.sellora.core.infrastructure.persistence.*;
import com.sellora.core.presentation.dtos.*;
import com.sellora.core.presentation.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final PromoCodeRepository promoCodeRepository;
  private final PromoUsageHistoryRepository promoUsageHistoryRepository;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  @Transactional
  public OrderResponseDto checkout(Long userId, CheckoutRequestDto request) {
    // 1. Перевірка юзера та магазину
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));
    if (!storeRepository.existsById(request.getStoreId())) {
      throw new ResourceNotFoundException("Магазин не знайдено");
    }

    // 2. Валідація доставки
    String deliveryType = request.getDeliveryType().toUpperCase();
    if (("BRANCH".equals(deliveryType) || "COURIER".equals(deliveryType))) {
      if (request.getCarrierId() == null || request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty()) {
        throw new BadRequestException("Для доставки BRANCH або COURIER обов'язково вказати carrier_id та delivery_address");
      }
    }

    // 3. Перевірка кошика та товарів
    Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new EmptyCartException("Кошик порожній"));
    List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
    if (cartItems.isEmpty()) throw new EmptyCartException("Кошик порожній");

    List<Long> requestProductIds = request.getItems().stream().map(CheckoutItemDto::productId).toList();
    List<Long> cartProductIds = cartItems.stream().map(ci -> ci.getProduct().getId()).toList();

    if (!cartProductIds.containsAll(requestProductIds)) {
      throw new ConflictException("Деякі передані товари відсутні в кошику");
    }

    // 4. БЛОКУВАННЯ ТОВАРІВ (FOR UPDATE) для уникнення Race Conditions
    List<Product> products = productRepository.findByIdsForUpdate(requestProductIds);
    if (products.size() != requestProductIds.size()) {
      throw new ResourceNotFoundException("Один або кілька товарів не знайдено в базі");
    }

    // 5. Валідація товарів (належність магазину, статус, сток)
    BigDecimal subtotal = BigDecimal.ZERO;
    Map<Long, Integer> requestQuantities = request.getItems().stream()
      .collect(Collectors.toMap(CheckoutItemDto::productId, CheckoutItemDto::quantity));

    for (Product p : products) {
      if (!p.getMerchantId().equals(request.getStoreId())) {
        throw new ConflictException("Товар " + p.getId() + " не належить обраному магазину");
      }
      if (!"ACTIVE".equalsIgnoreCase(p.getStatus())) {
        throw new ConflictException("Товар '" + p.getTitle() + "' недоступний (Статус: " + p.getStatus() + ")");
      }
      int requestedQty = requestQuantities.get(p.getId());
      if (requestedQty > p.getStockQuantity()) {
        throw new ConflictException("Недостатньо на складі товару '" + p.getTitle() + "'. Доступно: " + p.getStockQuantity());
      }

      subtotal = subtotal.add(p.getStandardPrice().multiply(BigDecimal.valueOf(requestedQty)));

      // Списання залишків
      p.setStockQuantity(p.getStockQuantity() - requestedQty);
      if (p.getStockQuantity() == 0) p.setStatus("OUT_OF_STOCK");
    }
    productRepository.saveAll(products);

    // 6. Обробка промокоду
    BigDecimal discount = BigDecimal.ZERO;
    PromoCode appliedPromo = null;
    if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
      appliedPromo = promoCodeRepository.findByCode(request.getPromoCode())
        .orElseThrow(() -> new ResourceNotFoundException("Промокод не знайдено"));

      if (!appliedPromo.getIsActive()) throw new ConflictException("Промокод неактивний");
      OffsetDateTime now = OffsetDateTime.now();
      if (appliedPromo.getStartDate() != null && now.isBefore(appliedPromo.getStartDate())) throw new ConflictException("Дія промокоду ще не почалась");
      if (appliedPromo.getEndDate() != null && now.isAfter(appliedPromo.getEndDate())) throw new ConflictException("Дія промокоду завершилась");
      if (appliedPromo.getUsageLimit() != null && appliedPromo.getUsedCount() >= appliedPromo.getUsageLimit()) throw new ConflictException("Ліміт використання вичерпано");
      if (promoUsageHistoryRepository.existsByUserIdAndPromoId(userId, appliedPromo.getId())) throw new ConflictException("Ви вже використовували цей промокод");

      if ("PERCENTAGE".equalsIgnoreCase(appliedPromo.getDiscountType())) {
        discount = subtotal.multiply(appliedPromo.getValue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
      } else {
        discount = appliedPromo.getValue();
      }
      if (discount.compareTo(subtotal) > 0) discount = subtotal; // Знижка не може бути більшою за ціну
    }

    // 7. Обчислення фінальних сум (Tax = 1% від (subtotal - discount))
    BigDecimal taxableAmount = subtotal.subtract(discount);
    BigDecimal tax = taxableAmount.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalAmount = taxableAmount.add(tax);

    // 8. Збереження замовлення
    Order order = new Order();
    order.setUserId(userId);
    order.setMerchantId(request.getStoreId());
    order.setPurchaseType("REGULAR");

    order.setBuyerName(request.getBuyerName());
    order.setBuyerSurname(request.getBuyerSurname());
    order.setBuyerPhone(request.getBuyerPhone());
    order.setBuyerEmail(request.getBuyerEmail());

    order.setDeliveryType(deliveryType);
    order.setCarrierId(request.getCarrierId());
    try {
      order.setDeliveryAddress(request.getDeliveryAddress() != null ? objectMapper.writeValueAsString(request.getDeliveryAddress()) : null);
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Невірний формат delivery_address");
    }

    order.setPaymentMethod(request.getPaymentMethod().toUpperCase());
    order.setOrderComment(request.getOrderComment());

    order.setSubtotal(subtotal);
    order.setDiscount(discount);
    order.setTax(tax);
    order.setFinalPrice(totalAmount);

    order.setPaymentStatus("PENDING");
    order.setShippingStatus("PENDING");

    Order savedOrder = orderRepository.save(order);

    // 9. Фіксація промокоду
    if (appliedPromo != null) {
      appliedPromo.setUsedCount(appliedPromo.getUsedCount() + 1);
      promoCodeRepository.save(appliedPromo);

      PromoUsageHistory usage = new PromoUsageHistory();
      usage.setPromoId(appliedPromo.getId());
      usage.setUserId(userId);
      usage.setOrderId(savedOrder.getId());
      promoUsageHistoryRepository.save(usage);
    }

    // 10. Перенесення OrderItems та очищення кошика
    List<CartItem> itemsToCheckout = cartItems.stream()
      .filter(ci -> requestProductIds.contains(ci.getProduct().getId())).toList();

    List<OrderItem> orderItems = itemsToCheckout.stream().map(cartItem -> {
      Product p = cartItem.getProduct();
      OrderItem oi = new OrderItem();
      oi.setOrderId(savedOrder.getId());
      oi.setProductId(p.getId());
      oi.setQuantity(requestQuantities.get(p.getId()));
      oi.setPriceSnapshot(p.getStandardPrice());
      oi.setTitleSnapshot(p.getTitle());
      oi.setImageSnapshot((p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0) : null);
      return oi;
    }).toList();

    orderItemRepository.saveAll(orderItems);
    cartItemRepository.deleteAll(itemsToCheckout); // Видаляємо тільки те, що купили

    // 11. Генерація LiqPay URL (якщо оплата картою)
    String paymentUrl = null;
    if ("ONLINE_CARD".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
      paymentUrl = paymentService.generatePaymentUrl(savedOrder.getId(), savedOrder.getFinalPrice());
    }

    // 12. Формування масиву товарів (items)
    List<OrderItemDto> itemDtos = orderItems.stream().map(oi -> new OrderItemDto(
      oi.getId(),
      oi.getProductId(),
      oi.getQuantity(),
      oi.getPriceSnapshot(), // unit_price
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())), // total_price
      oi.getTitleSnapshot(),
      oi.getImageSnapshot()
    )).toList();

    // 13. Парсимо адресу назад в об'єкт, щоб фронтенд отримав гарний JSON, а не строку з бекслешами
    Object deliveryAddressJson = null;
    if (savedOrder.getDeliveryAddress() != null) {
      try {
        deliveryAddressJson = objectMapper.readValue(savedOrder.getDeliveryAddress(), Object.class);
      } catch (JsonProcessingException e) {
        // Якщо помилка, передаємо як є
        deliveryAddressJson = savedOrder.getDeliveryAddress();
      }
    }

    // 14. ЗБИРАЄМО ПОВНИЙ DTO ДЛЯ ФРОНТЕНДУ
    return new OrderResponseDto(
      savedOrder.getId(),
      savedOrder.getPurchaseType(),
      savedOrder.getSessionId(),
      savedOrder.getUserId(),
      savedOrder.getMerchantId(),

      savedOrder.getBuyerName(),
      savedOrder.getBuyerSurname(),
      savedOrder.getBuyerPhone(),
      savedOrder.getBuyerEmail(),

      savedOrder.getDeliveryType(),
      savedOrder.getCarrierId(),
      deliveryAddressJson, // Передаємо розпарсений JSON
      savedOrder.getTrackingNumber(),
      savedOrder.getPaymentMethod(),
      savedOrder.getOrderComment(),

      savedOrder.getSubtotal(),
      savedOrder.getTax(),
      savedOrder.getDiscount(),
      savedOrder.getFinalPrice(),

      savedOrder.getPaymentStatus(),
      savedOrder.getShippingStatus(),
      paymentUrl,

      savedOrder.getCreatedAt(),
      savedOrder.getUpdatedAt(),

      itemDtos
    );
  }

  @Transactional(readOnly = true)
  public Page<OrderPreviewDto> getUserOrdersHistory(Long userId, Pageable pageable) {
    // 1. Отримуємо сторінку замовлень користувача
    Page<Order> ordersPage = orderRepository.findAllByUserId(userId, pageable);

    if (ordersPage.isEmpty()) {
      return Page.empty(pageable);
    }

    // 2. Збираємо всі ID цих замовлень
    List<Long> orderIds = ordersPage.getContent().stream()
      .map(Order::getId)
      .toList();

    // 3. Дістаємо всі OrderItems для цих замовлень ОДНИМ запитом
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);

    // 4. Групуємо товари по orderId для швидкого доступу
    Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
      .collect(Collectors.groupingBy(OrderItem::getOrderId));

    // 5. Мапимо замовлення у DTO для фронтенду
    return ordersPage.map(order -> {
      List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), List.of());

      List<OrderItemPreviewDto> previewItems = orderItems.stream()
        .map(item -> new OrderItemPreviewDto(
          item.getImageSnapshot(),
          item.getTitleSnapshot()
        ))
        .toList();

      return new OrderPreviewDto(
        order.getId(),
        order.getPurchaseType(),
        order.getMerchantId(),
        order.getFinalPrice(),
        order.getPaymentStatus(),
        order.getShippingStatus(),
        order.getCreatedAt(),
        previewItems
      );
    });
  }

  @Transactional(readOnly = true)
  public Page<StoreOrderPreviewDto> getStoreOrders(Long storeId, Long requesterId, String paymentStatus, String shippingStatus, Pageable pageable) {

    // Перевірка прав доступу: Requester має бути власником магазину або ADMIN
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    User requester = userRepository.findById(requesterId)
      .orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

    if (!store.getOwnerId().equals(requesterId) && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Ви не маєте доступу до замовлень цього магазину");
    }

    // Отримання списку замовлень
    Page<Order> ordersPage = orderRepository.findStoreOrdersWithFilters(storeId, paymentStatus, shippingStatus, pageable);

    if (ordersPage.isEmpty()) return Page.empty(pageable);

    // Оптимізована вибірка товарів
    List<Long> orderIds = ordersPage.getContent().stream().map(Order::getId).toList();
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
    Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

    // Мапінг у DTO для продавця (з ім'ям і телефоном)
    return ordersPage.map(order -> {
      List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), List.of());
      List<OrderItemPreviewDto> previewItems = orderItems.stream()
        .map(item -> new OrderItemPreviewDto(item.getImageSnapshot(), item.getTitleSnapshot()))
        .toList();

      return new StoreOrderPreviewDto(
        order.getId(),
        order.getPurchaseType(),
        order.getBuyerName(),
        order.getBuyerSurname(),
        order.getBuyerPhone(),
        order.getFinalPrice(),
        order.getPaymentStatus(),
        order.getShippingStatus(),
        order.getCreatedAt(),
        previewItems
      );
    });
  }

  @Transactional(readOnly = true)
  public OrderResponseDto getOrderDetails(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));

    User requester = userRepository.findById(requesterId)
      .orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

    // Перевіряємо чи є користувач власником магазину
    Store store = storeRepository.findById(order.getMerchantId()).orElse(null);
    boolean isStoreOwner = store != null && store.getOwnerId().equals(requesterId);

    // АВТОРИЗАЦІЯ: Тільки покупець, продавець (власник магазину) або ADMIN
    if (!order.getUserId().equals(requesterId) && !isStoreOwner && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Ви не маєте доступу до деталей цього замовлення");
    }

    // Отримуємо товари замовлення
    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
    List<OrderItemDto> itemDtos = items.stream().map(oi -> new OrderItemDto(
      oi.getId(), oi.getProductId(), oi.getQuantity(), oi.getPriceSnapshot(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())),
      oi.getTitleSnapshot(), oi.getImageSnapshot()
    )).toList();

    // Парсинг JSONB адреси
    Object deliveryAddressJson = null;
    if (order.getDeliveryAddress() != null) {
      try {
        deliveryAddressJson = objectMapper.readValue(order.getDeliveryAddress(), Object.class);
      } catch (JsonProcessingException e) {
        deliveryAddressJson = order.getDeliveryAddress();
      }
    }

    return new OrderResponseDto(
      order.getId(), order.getPurchaseType(), order.getSessionId(), order.getUserId(), order.getMerchantId(),
      order.getBuyerName(), order.getBuyerSurname(), order.getBuyerPhone(), order.getBuyerEmail(),
      order.getDeliveryType(), order.getCarrierId(), deliveryAddressJson, order.getTrackingNumber(),
      order.getPaymentMethod(), order.getOrderComment(), order.getSubtotal(), order.getTax(),
      order.getDiscount(), order.getFinalPrice(), order.getPaymentStatus(), order.getShippingStatus(),
      null, order.getCreatedAt(), order.getUpdatedAt(), itemDtos
    );
  }
}
