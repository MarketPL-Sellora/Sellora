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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
  private final GroupBuySessionRepository groupBuySessionRepository;
  private final TransactionEventRepository transactionEventRepository;
  private final GroupMemberRepository groupMemberRepository;
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
    Page<Order> ordersPage = orderRepository.findAllByUserId(userId, pageable);

    if (ordersPage.isEmpty()) {
      return Page.empty(pageable);
    }

    List<Long> orderIds = ordersPage.getContent().stream()
      .map(Order::getId)
      .toList();

    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
    Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
      .collect(Collectors.groupingBy(OrderItem::getOrderId));

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
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    User requester = userRepository.findById(requesterId)
      .orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

    if (!store.getOwnerId().equals(requesterId) && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Ви не маєте доступу до замовлень цього магазину");
    }

    Page<Order> ordersPage = orderRepository.findStoreOrdersWithFilters(storeId, paymentStatus, shippingStatus, pageable);
    if (ordersPage.isEmpty()) return Page.empty(pageable);

    List<Long> orderIds = ordersPage.getContent().stream().map(Order::getId).toList();
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
    Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

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

    Store store = storeRepository.findById(order.getMerchantId()).orElse(null);
    boolean isStoreOwner = store != null && store.getOwnerId().equals(requesterId);

    if (!order.getUserId().equals(requesterId) && !isStoreOwner && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Ви не маєте доступу до деталей цього замовлення");
    }

    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
    List<OrderItemDto> itemDtos = items.stream().map(oi -> new OrderItemDto(
      oi.getId(), oi.getProductId(), oi.getQuantity(), oi.getPriceSnapshot(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())),
      oi.getTitleSnapshot(), oi.getImageSnapshot()
    )).toList();

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

  @Transactional
  public OrderResponseDto updateOrder(Long orderId, Long requesterId, UpdateOrderRequestDto request) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));

    Store store = storeRepository.findById(order.getMerchantId())
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    // Перевірка, що юзер — власник магазину
    if (!store.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Тільки власник магазину може оновлювати це замовлення");
    }

    // --- ДОДАНО: ЗАХИСТ ВІД "ОЖИВЛЕННЯ" СКАСОВАНИХ ЗАМОВЛЕНЬ ---
    // Якщо замовлення вже скасоване, будь-які зміни заборонені
    if ("CANCELLED".equalsIgnoreCase(order.getPaymentStatus()) || "CANCELLED".equalsIgnoreCase(order.getShippingStatus())) {
      throw new BadRequestException("Скасоване замовлення є остаточним і не підлягає редагуванню");
    }
    // -----------------------------------------------------------

    // Оновлення Payment Status
    if (request.paymentStatus() != null) {
      if ("ONLINE_CARD".equalsIgnoreCase(order.getPaymentMethod())) {
        throw new BadRequestException("Статус оплати для онлайн-оплати змінюється автоматично через платіжну систему");
      }
      if ("CASH_ON_DELIVERY".equalsIgnoreCase(order.getPaymentMethod())) {
        String newPaymentStatus = request.paymentStatus().toUpperCase();
        if (!List.of("PENDING", "PAID", "CANCELLED").contains(newPaymentStatus)) {
          throw new BadRequestException("Недопустимий статус оплати. Дозволено: PENDING, PAID, CANCELLED");
        }
        order.setPaymentStatus(newPaymentStatus);
      }
    }

    // Оновлення Shipping Status
    if (request.shippingStatus() != null) {
      String newShippingStatus = request.shippingStatus().toUpperCase();
      if (!List.of("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(newShippingStatus)) {
        throw new BadRequestException("Недопустимий статус доставки");
      }
      order.setShippingStatus(newShippingStatus);
    }

    // Оновлення Tracking Number
    if (request.trackingNumber() != null) {
      String tracking = request.trackingNumber().trim();
      if (tracking.isEmpty()) {
        throw new BadRequestException("Трек-номер не може бути порожнім");
      }
      order.setTrackingNumber(tracking);
    }

    orderRepository.save(order);

    return getOrderDetails(orderId, requesterId);
  }

  @Transactional(readOnly = true)
  public PaymentUrlResponseDto retryPayment(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));

    // 1. Перевірка доступу (тільки покупець)
    if (!order.getUserId().equals(requesterId)) {
      throw new ForbiddenException("Ви не маєте доступу до цього замовлення");
    }

    // 2. Валідація методу оплати
    if (!"ONLINE_CARD".equalsIgnoreCase(order.getPaymentMethod())) {
      throw new BadRequestException("Повторна оплата доступна тільки для замовлень з онлайн-оплатою");
    }

    // 3. Валідація статусу оплати
    if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
      throw new BadRequestException("Замовлення вже оплачено або скасовано");
    }

    // 4. Генерація нового посилання
    String paymentUrl = paymentService.generatePaymentUrl(order.getId(), order.getFinalPrice());
    return new PaymentUrlResponseDto(paymentUrl);
  }

  @Transactional
  public OrderCancelResponseDto cancelOrder(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));

    // 1. Авторизація (тільки покупець)
    if (!order.getUserId().equals(requesterId)) {
      throw new ForbiddenException("Ви не можете скасувати чуже замовлення");
    }

    // 2. Валідація статусів
    if (!"PENDING".equalsIgnoreCase(order.getShippingStatus())) {
      throw new BadRequestException("Неможливо скасувати замовлення, яке вже обробляється або відправлено");
    }
    if ("CANCELLED".equalsIgnoreCase(order.getPaymentStatus()) || "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
      throw new BadRequestException("Замовлення вже скасовано");
    }

    // ВИКЛИКАЄМО СПІЛЬНУ ЛОГІКУ
    processOrderCancellationLogic(order);

    return new OrderCancelResponseDto(
      order.getId(),
      order.getPaymentStatus(),
      order.getShippingStatus(),
      "Замовлення успішно скасовано"
    );
  }

  // --- СПІЛЬНА ЛОГІКА ДЛЯ РУЧНОГО ТА АВТОМАТИЧНОГО СКАСУВАННЯ ---
  public void processOrderCancellationLogic(Order order) {
    // 1. Повернення товару на склад
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    for (OrderItem item : items) {
      Product product = productRepository.findByIdForUpdate(item.getProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

      product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
      if ("OUT_OF_STOCK".equalsIgnoreCase(product.getStatus()) && product.getStockQuantity() > 0) {
        product.setStatus("ACTIVE");
      }
      productRepository.save(product);
    }

    // 2. Повернення промокоду (якщо був використаний)
    promoUsageHistoryRepository.findByOrderId(order.getId()).ifPresent(history -> {
      promoCodeRepository.findById(history.getPromoId()).ifPresent(promoCode -> {
        promoCode.setUsedCount(Math.max(0, promoCode.getUsedCount() - 1));
        promoCodeRepository.save(promoCode);
      });
      promoUsageHistoryRepository.delete(history);
    });

    // 3. Логіка для GROUP_BUY
    if ("GROUP_BUY".equalsIgnoreCase(order.getPurchaseType()) && order.getSessionId() != null) {
      groupMemberRepository.findBySessionIdAndUserId(order.getSessionId(), order.getUserId()).ifPresent(member -> {
        groupMemberRepository.delete(member);

        int remainingMembers = groupMemberRepository.countBySessionId(order.getSessionId()) - 1;
        if (remainingMembers <= 0) {
          groupBuySessionRepository.findById(order.getSessionId()).ifPresent(session -> {
            session.setStatus("CANCELLED");
            groupBuySessionRepository.save(session);
          });
        }
      });
    }

    // 4. Оновлення статусів замовлення
    order.setPaymentStatus("CANCELLED");
    order.setShippingStatus("CANCELLED");
    orderRepository.save(order);
  }

  // --- МЕТОД ДЛЯ ПЛАНУВАЛЬНИКА ---
  @Transactional
  public int cancelExpiredPendingOrders() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(24);
    List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(threshold);

    for (Order order : expiredOrders) {
      processOrderCancellationLogic(order);
    }

    return expiredOrders.size(); // Повертаємо кількість скасованих
  }

  @Transactional(readOnly = true)
  public List<TransactionEventDto> getOrderTransactions(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));

    Store store = storeRepository.findById(order.getMerchantId())
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    User requester = userRepository.findById(requesterId)
      .orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

    // Перевірка доступу: тільки власник магазину або ADMIN
    if (!store.getOwnerId().equals(requesterId) && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Тільки власник магазину або адміністратор може переглядати транзакції");
    }

    // Отримуємо відсортовані події
    List<TransactionEvent> events = transactionEventRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

    // Мапимо у DTO
    return events.stream().map(event -> new TransactionEventDto(
      event.getId(),
      event.getIdempotencyKey(),
      event.getOrderId(),
      event.getEventType(),
      event.getAmount(),
      event.getCreatedAt()
    )).toList();
  }
}
