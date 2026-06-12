package com.sellora.core.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.domain.entities.*;
import com.sellora.core.infrastructure.persistence.*;
import com.sellora.core.presentation.dtos.*;
import com.sellora.core.presentation.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock OrderRepository orderRepository;
  @Mock CartRepository cartRepository;
  @Mock CartItemRepository cartItemRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock ProductRepository productRepository;
  @Mock UserRepository userRepository;
  @Mock StoreRepository storeRepository;
  @Mock PromoCodeRepository promoCodeRepository;
  @Mock PromoUsageHistoryRepository promoUsageHistoryRepository;
  @Mock PaymentService paymentService;

  @Spy ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks OrderService orderService;

  // ─── Допоміжні фабрики ────────────────────────────────────────────────────

  private User makeUser(Long id) {
    User u = new User();
    u.setId(id);
    u.setRole("USER");
    return u;
  }

  private Product makeProduct(Long id, Long storeId, BigDecimal price, int stock) {
    Product p = new Product();
    p.setId(id);
    p.setMerchantId(storeId);
    p.setStandardPrice(price);
    p.setStockQuantity(stock);
    p.setStatus("ACTIVE");
    p.setTitle("Product " + id);
    p.setImages(List.of("img-url-" + id));
    return p;
  }

  private Cart makeCart(Long cartId, Long userId) {
    Cart c = new Cart();
    c.setId(cartId);
    c.setUserId(userId);
    return c;
  }

  private CartItem makeCartItem(Cart cart, Product product) {
    CartItem ci = new CartItem();
    ci.setCart(cart);
    ci.setProduct(product);
    ci.setQuantity(2);
    return ci;
  }

  private CheckoutRequestDto makeRequest(Long storeId, List<CheckoutItemDto> items) {
    CheckoutRequestDto dto = new CheckoutRequestDto();
    dto.setStoreId(storeId);
    dto.setBuyerName("Іван");
    dto.setBuyerSurname("Іваненко");
    dto.setBuyerPhone("+380501234567");
    dto.setBuyerEmail("ivan@example.com");
    dto.setDeliveryType("PICKUP");
    dto.setPaymentMethod("CASH_ON_DELIVERY");
    dto.setItems(items);
    return dto;
  }

  private Order makeSavedOrder(Long orderId, Long userId, Long storeId) {
    Order o = new Order();
    o.setId(orderId);
    o.setUserId(userId);
    o.setMerchantId(storeId);
    o.setPurchaseType("REGULAR");
    o.setBuyerName("Іван");
    o.setBuyerSurname("Іваненко");
    o.setBuyerPhone("+380501234567");
    o.setBuyerEmail("ivan@example.com");
    o.setDeliveryType("PICKUP");
    o.setPaymentMethod("CASH_ON_DELIVERY");
    o.setSubtotal(new BigDecimal("200.00"));
    o.setDiscount(BigDecimal.ZERO);
    o.setTax(new BigDecimal("1.98"));
    o.setFinalPrice(new BigDecimal("201.98"));
    o.setPaymentStatus("PENDING");
    o.setShippingStatus("PENDING");
    return o;
  }

  // ─── checkout ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("checkout()")
  class Checkout {

    @Test
    @DisplayName("успішне оформлення замовлення без промокоду (PICKUP)")
    void success_noPromo() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("100.00"), 5);
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutItemDto itemDto = new CheckoutItemDto(productId, 2);
      CheckoutRequestDto request = makeRequest(storeId, List.of(itemDto));
      Order savedOrder = makeSavedOrder(200L, userId, storeId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

      OrderResponseDto result = orderService.checkout(userId, request);

      assertThat(result).isNotNull();
      assertThat(result.paymentStatus()).isEqualTo("PENDING");
      assertThat(result.shippingStatus()).isEqualTo("PENDING");

      // Перевіряємо що сток зменшився
      assertThat(product.getStockQuantity()).isEqualTo(3);
      verify(productRepository).saveAll(anyList());
      verify(cartItemRepository).deleteAll(anyList());
    }

    @Test
    @DisplayName("успішне оформлення з PERCENTAGE промокодом")
    void success_percentagePromo() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("100.00"), 5);
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutItemDto itemDto = new CheckoutItemDto(productId, 2);
      CheckoutRequestDto request = makeRequest(storeId, List.of(itemDto));
      request.setPromoCode("SAVE10");

      PromoCode promo = new PromoCode();
      promo.setId(1L);
      promo.setCode("SAVE10");
      promo.setIsActive(true);
      promo.setDiscountType("PERCENTAGE");
      promo.setValue(new BigDecimal("10"));
      promo.setUsedCount(0);

      Order savedOrder = makeSavedOrder(200L, userId, storeId);
      savedOrder.setDiscount(new BigDecimal("20.00")); // 10% від 200

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));
      when(promoCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(promo));
      when(promoUsageHistoryRepository.existsByUserIdAndPromoId(userId, promo.getId())).thenReturn(false);
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

      OrderResponseDto result = orderService.checkout(userId, request);

      assertThat(result.discount()).isEqualByComparingTo(new BigDecimal("20.00"));
      verify(promoCodeRepository).save(promo);
      verify(promoUsageHistoryRepository).save(any(PromoUsageHistory.class));
      assertThat(promo.getUsedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("коли сток дорівнює 0 після покупки — статус OUT_OF_STOCK")
    void product_becomesOutOfStock() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("50.00"), 2); // рівно 2 штуки
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutItemDto itemDto = new CheckoutItemDto(productId, 2);
      CheckoutRequestDto request = makeRequest(storeId, List.of(itemDto));
      Order savedOrder = makeSavedOrder(200L, userId, storeId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

      orderService.checkout(userId, request);

      assertThat(product.getStockQuantity()).isZero();
      assertThat(product.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    // ─── негативні сценарії ────────────────────────────────────────────────

    @Test
    @DisplayName("кидає ResourceNotFoundException коли юзера не знайдено")
    void throws_whenUserNotFound() {
      when(userRepository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> orderService.checkout(99L, makeRequest(1L, List.of())))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Користувача не знайдено");
    }

    @Test
    @DisplayName("кидає ResourceNotFoundException коли магазин не існує")
    void throws_whenStoreNotFound() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
      when(storeRepository.existsById(10L)).thenReturn(false);

      assertThatThrownBy(() -> orderService.checkout(1L, makeRequest(10L, List.of())))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Магазин не знайдено");
    }

    @Test
    @DisplayName("кидає BadRequestException коли BRANCH доставка без carrierId")
    void throws_whenBranchDeliveryMissingCarrier() {
      CheckoutRequestDto request = makeRequest(10L, List.of(new CheckoutItemDto(100L, 1)));
      request.setDeliveryType("BRANCH");
      request.setCarrierId(null);
      request.setDeliveryAddress(Map.of("city", "Київ"));

      when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
      when(storeRepository.existsById(10L)).thenReturn(true);

      assertThatThrownBy(() -> orderService.checkout(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("carrier_id");
    }

    @Test
    @DisplayName("кидає EmptyCartException коли кошик порожній")
    void throws_whenCartEmpty() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
      when(storeRepository.existsById(10L)).thenReturn(true);
      when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> orderService.checkout(1L, makeRequest(10L, List.of(new CheckoutItemDto(100L, 1)))))
        .isInstanceOf(EmptyCartException.class);
    }

    @Test
    @DisplayName("кидає ConflictException коли товар не у кошику")
    void throws_whenProductNotInCart() {
      Cart cart = makeCart(50L, 1L);
      Product otherProduct = makeProduct(999L, 10L, BigDecimal.TEN, 5);
      CartItem cartItem = makeCartItem(cart, otherProduct);

      when(userRepository.findById(1L)).thenReturn(Optional.of(makeUser(1L)));
      when(storeRepository.existsById(10L)).thenReturn(true);
      when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));

      CheckoutRequestDto request = makeRequest(10L, List.of(new CheckoutItemDto(100L, 1))); // productId=100, якого нема в кошику

      assertThatThrownBy(() -> orderService.checkout(1L, request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("відсутні в кошику");
    }

    @Test
    @DisplayName("кидає ConflictException коли недостатньо товару на складі")
    void throws_whenInsufficientStock() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("50.00"), 1); // лише 1 штука
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutItemDto itemDto = new CheckoutItemDto(productId, 5); // хочуть 5

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));

      assertThatThrownBy(() -> orderService.checkout(userId, makeRequest(storeId, List.of(itemDto))))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Недостатньо на складі");
    }

    @Test
    @DisplayName("кидає ConflictException коли товар не ACTIVE")
    void throws_whenProductNotActive() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("50.00"), 5);
      product.setStatus("INACTIVE");
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));

      assertThatThrownBy(() -> orderService.checkout(userId, makeRequest(storeId, List.of(new CheckoutItemDto(productId, 1)))))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("недоступний");
    }

    @Test
    @DisplayName("кидає ConflictException коли товар належить іншому магазину")
    void throws_whenProductBelongsToDifferentStore() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, 99L, new BigDecimal("50.00"), 5); // storeId=99, не 10
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));

      assertThatThrownBy(() -> orderService.checkout(userId, makeRequest(storeId, List.of(new CheckoutItemDto(productId, 1)))))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("не належить обраному магазину");
    }

    @Test
    @DisplayName("кидає ConflictException якщо промокод неактивний")
    void throws_whenPromoInactive() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("100.00"), 5);
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutRequestDto request = makeRequest(storeId, List.of(new CheckoutItemDto(productId, 1)));
      request.setPromoCode("DEAD");

      PromoCode promo = new PromoCode();
      promo.setId(1L);
      promo.setCode("DEAD");
      promo.setIsActive(false);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));
      when(promoCodeRepository.findByCode("DEAD")).thenReturn(Optional.of(promo));

      assertThatThrownBy(() -> orderService.checkout(userId, request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Промокод неактивний");
    }

    @Test
    @DisplayName("кидає ConflictException якщо юзер вже використовував промокод")
    void throws_whenPromoAlreadyUsedByUser() {
      Long userId = 1L, storeId = 10L, productId = 100L;

      Product product = makeProduct(productId, storeId, new BigDecimal("100.00"), 5);
      Cart cart = makeCart(50L, userId);
      CartItem cartItem = makeCartItem(cart, product);
      CheckoutRequestDto request = makeRequest(storeId, List.of(new CheckoutItemDto(productId, 1)));
      request.setPromoCode("USED");

      PromoCode promo = new PromoCode();
      promo.setId(5L);
      promo.setCode("USED");
      promo.setIsActive(true);
      promo.setDiscountType("FIXED");
      promo.setValue(new BigDecimal("20"));
      promo.setUsedCount(0);

      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.existsById(storeId)).thenReturn(true);
      when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
      when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
      when(productRepository.findByIdsForUpdate(List.of(productId))).thenReturn(List.of(product));
      when(promoCodeRepository.findByCode("USED")).thenReturn(Optional.of(promo));
      when(promoUsageHistoryRepository.existsByUserIdAndPromoId(userId, promo.getId())).thenReturn(true);

      assertThatThrownBy(() -> orderService.checkout(userId, request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("вже використовували");
    }
  }

  // ─── getUserOrdersHistory ──────────────────────────────────────────────────

  @Nested
  @DisplayName("getUserOrdersHistory()")
  class GetUserOrdersHistory {

    @Test
    @DisplayName("повертає сторінку з OrderPreviewDto і підтягує items")
    void returnsPageWithItems() {
      Long userId = 1L;
      Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

      Order order = makeSavedOrder(200L, userId, 10L);
      Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);

      OrderItem item = new OrderItem();
      item.setId(1L);
      item.setOrderId(200L);
      item.setTitleSnapshot("Товар 1");
      item.setImageSnapshot("img.jpg");

      when(orderRepository.findAllByUserId(userId, pageable)).thenReturn(page);
      when(orderItemRepository.findByOrderIdIn(List.of(200L))).thenReturn(List.of(item));

      Page<OrderPreviewDto> result = orderService.getUserOrdersHistory(userId, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      OrderPreviewDto dto = result.getContent().get(0);
      assertThat(dto.id()).isEqualTo(200L);
      assertThat(dto.itemsPreview()).hasSize(1);
      assertThat(dto.itemsPreview().get(0).titleSnapshot()).isEqualTo("Товар 1");
    }

    @Test
    @DisplayName("повертає порожню сторінку якщо замовлень немає")
    void returnsEmptyPage() {
      Long userId = 1L;
      Pageable pageable = PageRequest.of(0, 10);
      when(orderRepository.findAllByUserId(userId, pageable)).thenReturn(Page.empty(pageable));

      Page<OrderPreviewDto> result = orderService.getUserOrdersHistory(userId, pageable);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ─── getOrderDetails ──────────────────────────────────────────────────────

  @Nested
  @DisplayName("getOrderDetails()")
  class GetOrderDetails {

    @Test
    @DisplayName("покупець бачить своє замовлення")
    void buyer_canSeeOwnOrder() {
      Long userId = 1L, orderId = 200L, storeId = 10L;
      Order order = makeSavedOrder(orderId, userId, storeId);

      Store store = new Store();
      store.setId(storeId);
      store.setOwnerId(99L); // інший власник

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(userRepository.findById(userId)).thenReturn(Optional.of(makeUser(userId)));
      when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
      when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

      OrderResponseDto result = orderService.getOrderDetails(orderId, userId);

      assertThat(result.id()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("власник магазину бачить замовлення свого магазину")
    void storeOwner_canSeeStoreOrder() {
      Long buyerId = 1L, ownerId = 2L, orderId = 200L, storeId = 10L;
      Order order = makeSavedOrder(orderId, buyerId, storeId);

      Store store = new Store();
      store.setId(storeId);
      store.setOwnerId(ownerId); // requester — власник

      User owner = makeUser(ownerId);
      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
      when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
      when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

      OrderResponseDto result = orderService.getOrderDetails(orderId, ownerId);
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("ADMIN бачить будь-яке замовлення")
    void admin_canSeeAnyOrder() {
      Long buyerId = 1L, adminId = 100L, orderId = 200L, storeId = 10L;
      Order order = makeSavedOrder(orderId, buyerId, storeId);

      Store store = new Store();
      store.setId(storeId);
      store.setOwnerId(99L);

      User admin = makeUser(adminId);
      admin.setRole("ADMIN");

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
      when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
      when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

      assertThatNoException().isThrownBy(() -> orderService.getOrderDetails(orderId, adminId));
    }

    @Test
    @DisplayName("сторонній юзер отримує ForbiddenException")
    void stranger_getForbidden() {
      Long buyerId = 1L, strangerId = 5L, orderId = 200L, storeId = 10L;
      Order order = makeSavedOrder(orderId, buyerId, storeId);

      Store store = new Store();
      store.setId(storeId);
      store.setOwnerId(99L);

      when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
      when(userRepository.findById(strangerId)).thenReturn(Optional.of(makeUser(strangerId)));
      when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

      assertThatThrownBy(() -> orderService.getOrderDetails(orderId, strangerId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("не маєте доступу");
    }

    @Test
    @DisplayName("кидає ResourceNotFoundException коли замовлення не знайдено")
    void throws_whenOrderNotFound() {
      when(orderRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> orderService.getOrderDetails(999L, 1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Замовлення не знайдено");
    }
  }
}
