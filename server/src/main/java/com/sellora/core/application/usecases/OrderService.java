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
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));
    if (!storeRepository.existsById(request.getStoreId())) {
      throw new ResourceNotFoundException("Магазин не знайдено");
    }

    String deliveryType = request.getDeliveryType().toUpperCase();
    if (("BRANCH".equals(deliveryType) || "COURIER".equals(deliveryType))) {
      if (request.getCarrierId() == null || request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty()) {
        throw new BadRequestException("Для доставки BRANCH або COURIER обов'язково вказати carrier_id та delivery_address");
      }
    }

    Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new EmptyCartException("Кошик порожній"));
    List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
    if (cartItems.isEmpty()) throw new EmptyCartException("Кошик порожній");

    List<Long> requestProductIds = request.getItems().stream().map(CheckoutItemDto::productId).toList();
    List<Long> cartProductIds = cartItems.stream().map(ci -> ci.getProduct().getId()).toList();

    if (!cartProductIds.containsAll(requestProductIds)) {
      throw new ConflictException("Деякі передані товари відсутні в кошику");
    }

    List<Product> products = productRepository.findByIdsForUpdate(requestProductIds);
    if (products.size() != requestProductIds.size()) {
      throw new ResourceNotFoundException("Один або кілька товарів не знайдено в базі");
    }

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

      p.setStockQuantity(p.getStockQuantity() - requestedQty);
      if (p.getStockQuantity() == 0) p.setStatus("OUT_OF_STOCK");
    }
    productRepository.saveAll(products);

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
      if (discount.compareTo(subtotal) > 0) discount = subtotal;
    }

    BigDecimal taxableAmount = subtotal.subtract(discount);
    BigDecimal tax = taxableAmount.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalAmount = taxableAmount.add(tax);

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

    if (appliedPromo != null) {
      appliedPromo.setUsedCount(appliedPromo.getUsedCount() + 1);
      promoCodeRepository.save(appliedPromo);

      PromoUsageHistory usage = new PromoUsageHistory();
      usage.setPromoId(appliedPromo.getId());
      usage.setUserId(userId);
      usage.setOrderId(savedOrder.getId());
      promoUsageHistoryRepository.save(usage);
    }

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
    cartItemRepository.deleteAll(itemsToCheckout);

    String paymentUrl = null;
    if ("ONLINE_CARD".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
      paymentUrl = paymentService.generatePaymentUrl(savedOrder.getId(), savedOrder.getFinalPrice());
    }

    List<OrderItemDto> itemDtos = orderItems.stream().map(oi -> new OrderItemDto(
      oi.getId(), oi.getProductId(), oi.getQuantity(), oi.getPriceSnapshot(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())), oi.getTitleSnapshot(), oi.getImageSnapshot()
    )).toList();

    Object deliveryAddressJson = null;
    if (savedOrder.getDeliveryAddress() != null) {
      try {
        deliveryAddressJson = objectMapper.readValue(savedOrder.getDeliveryAddress(), Object.class);
      } catch (JsonProcessingException e) {
        deliveryAddressJson = savedOrder.getDeliveryAddress();
      }
    }

    return new OrderResponseDto(
      savedOrder.getId(), savedOrder.getPurchaseType(), savedOrder.getSessionId(), savedOrder.getUserId(), savedOrder.getMerchantId(),
      savedOrder.getBuyerName(), savedOrder.getBuyerSurname(), savedOrder.getBuyerPhone(), savedOrder.getBuyerEmail(),
      savedOrder.getDeliveryType(), savedOrder.getCarrierId(), deliveryAddressJson, savedOrder.getTrackingNumber(),
      savedOrder.getPaymentMethod(), savedOrder.getOrderComment(), savedOrder.getSubtotal(), savedOrder.getTax(),
      savedOrder.getDiscount(), savedOrder.getFinalPrice(), savedOrder.getPaymentStatus(), savedOrder.getShippingStatus(),
      paymentUrl, savedOrder.getCreatedAt(), savedOrder.getUpdatedAt(), itemDtos
    );
  }

  @Transactional(readOnly = true)
  public Page<OrderPreviewDto> getUserOrdersHistory(Long userId, Pageable pageable) {
    Page<Order> ordersPage = orderRepository.findAllByUserId(userId, pageable);
    if (ordersPage.isEmpty()) return Page.empty(pageable);

    List<Long> orderIds = ordersPage.getContent().stream().map(Order::getId).toList();
    List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
    Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

    return ordersPage.map(order -> {
      List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), List.of());
      List<OrderItemPreviewDto> previewItems = orderItems.stream()
        .map(item -> new OrderItemPreviewDto(item.getImageSnapshot(), item.getTitleSnapshot())).toList();

      return new OrderPreviewDto(
        order.getId(), order.getPurchaseType(), order.getMerchantId(), order.getFinalPrice(),
        order.getPaymentStatus(), order.getShippingStatus(), order.getCreatedAt(), previewItems
      );
    });
  }

  @Transactional(readOnly = true)
  public Page<StoreOrderPreviewDto> getStoreOrders(Long storeId, Long requesterId, String paymentStatus, String shippingStatus, Pageable pageable) {
    Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));
    User requester = userRepository.findById(requesterId).orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

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
        .map(item -> new OrderItemPreviewDto(item.getImageSnapshot(), item.getTitleSnapshot())).toList();

      return new StoreOrderPreviewDto(
        order.getId(), order.getPurchaseType(), order.getBuyerName(), order.getBuyerSurname(), order.getBuyerPhone(),
        order.getFinalPrice(), order.getPaymentStatus(), order.getShippingStatus(), order.getCreatedAt(), previewItems
      );
    });
  }

  @Transactional(readOnly = true)
  public OrderResponseDto getOrderDetails(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));
    User requester = userRepository.findById(requesterId).orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));
    Store store = storeRepository.findById(order.getMerchantId()).orElse(null);
    boolean isStoreOwner = store != null && store.getOwnerId().equals(requesterId);

    if (!order.getUserId().equals(requesterId) && !isStoreOwner && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Ви не маєте доступу до деталей цього замовлення");
    }

    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
    List<OrderItemDto> itemDtos = items.stream().map(oi -> new OrderItemDto(
      oi.getId(), oi.getProductId(), oi.getQuantity(), oi.getPriceSnapshot(),
      oi.getPriceSnapshot().multiply(BigDecimal.valueOf(oi.getQuantity())), oi.getTitleSnapshot(), oi.getImageSnapshot()
    )).toList();

    Object deliveryAddressJson = null;
    if (order.getDeliveryAddress() != null) {
      try { deliveryAddressJson = objectMapper.readValue(order.getDeliveryAddress(), Object.class); }
      catch (JsonProcessingException e) { deliveryAddressJson = order.getDeliveryAddress(); }
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
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));
    Store store = storeRepository.findById(order.getMerchantId()).orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    if (!store.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Тільки власник магазину може оновлювати це замовлення");
    }

    if ("CANCELLED".equalsIgnoreCase(order.getPaymentStatus()) || "CANCELLED".equalsIgnoreCase(order.getShippingStatus())) {
      throw new BadRequestException("Скасоване замовлення є остаточним і не підлягає редагуванню");
    }

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

    if (request.shippingStatus() != null) {
      String newShippingStatus = request.shippingStatus().toUpperCase();
      if (!List.of("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(newShippingStatus)) {
        throw new BadRequestException("Недопустимий статус доставки");
      }
      order.setShippingStatus(newShippingStatus);
    }

    if (request.trackingNumber() != null) {
      String tracking = request.trackingNumber().trim();
      if (tracking.isEmpty()) throw new BadRequestException("Трек-номер не може бути порожнім");
      order.setTrackingNumber(tracking);
    }

    orderRepository.save(order);
    return getOrderDetails(orderId, requesterId);
  }

  @Transactional(readOnly = true)
  public PaymentUrlResponseDto retryPayment(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));
    if (!order.getUserId().equals(requesterId)) throw new ForbiddenException("Ви не маєте доступу до цього замовлення");
    if (!"ONLINE_CARD".equalsIgnoreCase(order.getPaymentMethod())) throw new BadRequestException("Повторна оплата доступна тільки для онлайн-оплат");
    if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) throw new BadRequestException("Замовлення вже оплачено або скасовано");

    return new PaymentUrlResponseDto(paymentService.generatePaymentUrl(order.getId(), order.getFinalPrice()));
  }

  @Transactional
  public OrderCancelResponseDto cancelOrder(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));
    if (!order.getUserId().equals(requesterId)) throw new ForbiddenException("Ви не можете скасувати чуже замовлення");
    if (!"PENDING".equalsIgnoreCase(order.getShippingStatus())) throw new BadRequestException("Неможливо скасувати замовлення, яке вже обробляється");
    if ("CANCELLED".equalsIgnoreCase(order.getPaymentStatus()) || "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
      throw new BadRequestException("Замовлення вже скасовано");
    }

    processOrderCancellationLogic(order);

    return new OrderCancelResponseDto(order.getId(), order.getPaymentStatus(), order.getShippingStatus(), "Замовлення успішно скасовано");
  }

  // --- ФІКС 2: Логіка фізичного видалення для GROUP_BUY ---
  public void processOrderCancellationLogic(Order order) {
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

    // 1. Повернення товару на склад
    for (OrderItem item : items) {
      Product product = productRepository.findByIdForUpdate(item.getProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

      product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
      if ("OUT_OF_STOCK".equalsIgnoreCase(product.getStatus()) && product.getStockQuantity() > 0) {
        product.setStatus("ACTIVE");
      }
      productRepository.save(product);
    }

    // 2. Повернення промокоду
    promoUsageHistoryRepository.findByOrderId(order.getId()).ifPresent(history -> {
      promoCodeRepository.findById(history.getPromoId()).ifPresent(promoCode -> {
        promoCode.setUsedCount(Math.max(0, promoCode.getUsedCount() - 1));
        promoCodeRepository.save(promoCode);
      });
      promoUsageHistoryRepository.delete(history);
    });

    // 3. ЖОРСТКЕ ВИДАЛЕННЯ ДЛЯ GROUP_BUY
    if ("GROUP_BUY".equalsIgnoreCase(order.getPurchaseType())) {
      if (order.getSessionId() != null) {
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

      // Видаляємо Items та сам Order з бази даних
      orderItemRepository.deleteAll(items);
      orderRepository.delete(order);

      return; // Виходимо, щоб не продовжувати зміну статусів
    }

    // 4. Оновлення статусів для REGULAR замовлень
    order.setPaymentStatus("CANCELLED");
    order.setShippingStatus("CANCELLED");
    orderRepository.save(order);
  }

  @Transactional
  public int cancelExpiredPendingOrders() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(24);
    List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(threshold);
    for (Order order : expiredOrders) {
      processOrderCancellationLogic(order);
    }
    return expiredOrders.size();
  }

  @Transactional(readOnly = true)
  public List<TransactionEventDto> getOrderTransactions(Long orderId, Long requesterId) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Замовлення не знайдено"));
    Store store = storeRepository.findById(order.getMerchantId()).orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));
    User requester = userRepository.findById(requesterId).orElseThrow(() -> new UnauthorizedException("Користувача не знайдено"));

    if (!store.getOwnerId().equals(requesterId) && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Тільки власник магазину або адміністратор може переглядати транзакції");
    }

    List<TransactionEvent> events = transactionEventRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    return events.stream().map(event -> new TransactionEventDto(
      event.getId(), event.getIdempotencyKey(), event.getOrderId(), event.getEventType(), event.getAmount(), event.getCreatedAt()
    )).toList();
  }
}
