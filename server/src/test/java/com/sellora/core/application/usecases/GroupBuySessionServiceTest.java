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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupBuySessionServiceTest {

  @Mock GroupBuySessionRepository sessionRepository;
  @Mock GroupMemberRepository memberRepository;
  @Mock ProductRepository productRepository;
  @Mock UserRepository userRepository;
  @Mock OrderRepository orderRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock PromoCodeRepository promoCodeRepository;
  @Mock PromoUsageHistoryRepository promoUsageHistoryRepository;
  @Mock PaymentService paymentService;

  @Spy ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks GroupBuySessionService sessionService;

  // ─── Допоміжні фабрики ───────────────────────────────────────────────────────

  private Product makeProduct(Long id, Long merchantId) {
    Product p = new Product();
    p.setId(id);
    p.setMerchantId(merchantId);
    p.setTitle("Product " + id);
    p.setStatus("ACTIVE");
    p.setStandardPrice(new BigDecimal("100.00"));
    p.setGroupPrice(new BigDecimal("80.00"));
    p.setGroupTargetSize(3);
    p.setStockQuantity(10);
    p.setImages(List.of("img.jpg"));
    return p;
  }

  private GroupBuySession makeSession(Long id, Long productId, String status) {
    GroupBuySession s = new GroupBuySession();
    s.setId(id);
    s.setUuid(UUID.randomUUID().toString());
    s.setProductId(productId);
    s.setInitiatorId(1L);
    s.setStatus(status);
    s.setLockedPrice(new BigDecimal("80.00"));
    s.setLockedTargetSize(3);
    return s;
  }

  private GroupBuyCheckoutRequestDto makeDto(Long productId) {
    GroupBuyCheckoutRequestDto dto = new GroupBuyCheckoutRequestDto();
    dto.setProductId(productId);
    dto.setBuyerName("Іван");
    dto.setBuyerSurname("Іваненко");
    dto.setBuyerPhone("+380501234567");
    dto.setBuyerEmail("ivan@example.com");
    dto.setDeliveryType("PICKUP");
    dto.setPaymentMethod("CASH_ON_DELIVERY");
    dto.setQuantity(1);
    return dto;
  }

  private Order makeSavedOrder(Long id, Long userId, Long sessionId) {
    Order o = new Order();
    o.setId(id);
    o.setUserId(userId);
    o.setSessionId(sessionId);
    o.setPurchaseType("GROUP_BUY");
    o.setSubtotal(new BigDecimal("80.00"));
    o.setDiscount(BigDecimal.ZERO);
    o.setTax(new BigDecimal("0.80"));
    o.setFinalPrice(new BigDecimal("80.80"));
    o.setPaymentStatus("WAITING_FOR_GROUP");
    o.setShippingStatus("PENDING");
    return o;
  }

  // Мок для getSessionDetails, який викликається всередині createSession/joinSession
  private void mockGetSessionDetails(GroupBuySession session, Long productId, int memberCount) {
    when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
    when(memberRepository.countBySessionId(session.getId())).thenReturn(memberCount);
    when(memberRepository.findBySessionId(session.getId())).thenReturn(List.of());
    Product p = makeProduct(productId, 10L);
    when(productRepository.findById(productId)).thenReturn(Optional.of(p));
  }

  // ─── getSessionDetails ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("getSessionDetails()")
  class GetSessionDetails {

    @Test
    @DisplayName("повертає деталі активної сесії")
    void returnsSessionDetails() {
      Long productId = 100L;
      GroupBuySession session = makeSession(10L, productId, "ACTIVE");
      Product product = makeProduct(productId, 10L);

      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      when(memberRepository.countBySessionId(10L)).thenReturn(1);
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      GroupBuySessionResponseDto result = sessionService.getSessionDetails(session.getUuid());

      assertThat(result.status()).isEqualTo("ACTIVE");
      assertThat(result.targetSize()).isEqualTo(3);
      assertThat(result.currentMembersCount()).isEqualTo(1);
      assertThat(result.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable = false коли група заповнена")
    void isAvailable_falseWhenFull() {
      Long productId = 100L;
      GroupBuySession session = makeSession(10L, productId, "ACTIVE");
      Product product = makeProduct(productId, 10L);

      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      when(memberRepository.countBySessionId(10L)).thenReturn(3); // lockedTargetSize = 3
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      GroupBuySessionResponseDto result = sessionService.getSessionDetails(session.getUuid());

      assertThat(result.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("кидає ResourceNotFoundException коли сесію не знайдено")
    void throws_whenSessionNotFound() {
      when(sessionRepository.findByUuid("bad-uuid")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> sessionService.getSessionDetails("bad-uuid"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Сесію не знайдено");
    }
  }

  // ─── createSession ───────────────────────────────────────────────────────────

  @Nested
  @DisplayName("createSession()")
  class CreateSession {

    @Test
    @DisplayName("успішне створення сесії — зберігає сесію та ордер")
    void success_createsSessionAndOrder() {
      Long userId = 1L, productId = 100L;
      GroupBuyCheckoutRequestDto dto = makeDto(productId);

      Product product = makeProduct(productId, 10L);
      GroupBuySession savedSession = makeSession(10L, productId, "ACTIVE");
      Order savedOrder = makeSavedOrder(200L, userId, 10L);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(memberRepository.isUserInActiveSessionForProduct(userId, productId)).thenReturn(false);
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
      when(sessionRepository.save(any())).thenReturn(savedSession);
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      // мок для getSessionDetails в кінці
      mockGetSessionDetails(savedSession, productId, 1);

      GroupBuySessionResponseDto result = sessionService.createSession(dto, userId);

      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo("ACTIVE");
      verify(sessionRepository).save(any(GroupBuySession.class));
      verify(orderRepository).save(any(Order.class));
      verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("використовує groupPrice якщо є, інакше standardPrice")
    void usesGroupPrice_whenAvailable() {
      Long userId = 1L, productId = 100L;
      GroupBuyCheckoutRequestDto dto = makeDto(productId);
      Product product = makeProduct(productId, 10L); // groupPrice = 80.00
      GroupBuySession savedSession = makeSession(10L, productId, "ACTIVE");
      Order savedOrder = makeSavedOrder(200L, userId, 10L);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(memberRepository.isUserInActiveSessionForProduct(userId, productId)).thenReturn(false);
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
      when(sessionRepository.save(any())).thenAnswer(inv -> {
        GroupBuySession s = inv.getArgument(0);
        assertThat(s.getLockedPrice()).isEqualByComparingTo("80.00");
        s.setId(10L);
        return savedSession;
      });
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      mockGetSessionDetails(savedSession, productId, 1);

      sessionService.createSession(dto, userId);

      verify(sessionRepository).save(any());
    }

    @Test
    @DisplayName("кидає BadRequestException якщо юзер вже в активній сесії на цей товар")
    void throws_whenUserAlreadyInActiveSession() {
      Long userId = 1L, productId = 100L;
      GroupBuyCheckoutRequestDto dto = makeDto(productId);

      // Залишаємо лише ці два моки
      when(userRepository.existsById(userId)).thenReturn(true);
      when(memberRepository.isUserInActiveSessionForProduct(userId, productId)).thenReturn(true);

      assertThatThrownBy(() -> sessionService.createSession(dto, userId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("вже берете участь в активній сесії");

      verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("кидає ResourceNotFoundException якщо юзера не існує")
    void throws_whenUserNotFound() {
      when(userRepository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> sessionService.createSession(makeDto(100L), 99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Користувача не знайдено");
    }

    @Test
    @DisplayName("кидає ConflictException якщо недостатньо товару на складі")
    void throws_whenInsufficientStock() {
      Long userId = 1L, productId = 100L;
      GroupBuyCheckoutRequestDto dto = makeDto(productId);
      dto.setQuantity(999);

      Product product = makeProduct(productId, 10L);
      product.setStockQuantity(1);
      GroupBuySession savedSession = makeSession(10L, productId, "ACTIVE");

      when(userRepository.existsById(userId)).thenReturn(true);
      when(memberRepository.isUserInActiveSessionForProduct(userId, productId)).thenReturn(false);
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
      when(sessionRepository.save(any())).thenReturn(savedSession);

      assertThatThrownBy(() -> sessionService.createSession(dto, userId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Недостатньо товару на складі");
    }
  }

  // ─── joinSession ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("joinSession()")
  class JoinSession {

    @Test
    @DisplayName("успішне приєднання — не останній учасник, сесія лишається ACTIVE")
    void success_notLastMember_sessionStaysActive() {
      Long userId = 2L, productId = 100L;
      GroupBuySession session = makeSession(10L, productId, "ACTIVE");
      GroupBuyCheckoutRequestDto dto = makeDto(productId);
      Order savedOrder = makeSavedOrder(200L, userId, 10L);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(memberRepository.existsBySessionIdAndUserId(10L, userId)).thenReturn(false);
      when(memberRepository.countBySessionId(10L)).thenReturn(1); // після join стане 2 < 3
      when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(makeProduct(productId, 10L)));
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      // getSessionDetails всередині
      when(productRepository.findById(productId)).thenReturn(Optional.of(makeProduct(productId, 10L)));
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      GroupBuySessionResponseDto result = sessionService.joinSession(session.getUuid(), userId, dto);

      assertThat(result.status()).isEqualTo("ACTIVE");
      verify(memberRepository).save(any(GroupMember.class));
      verify(sessionRepository, never()).save(any(GroupBuySession.class)); // статус не змінюється
    }

    @Test
    @DisplayName("успішне приєднання — останній учасник, сесія стає COMPLETED")
    void success_lastMember_sessionCompleted() {
      Long userId = 3L, productId = 100L;
      GroupBuySession session = makeSession(10L, productId, "ACTIVE");
      GroupBuyCheckoutRequestDto dto = makeDto(productId);
      Order savedOrder = makeSavedOrder(200L, userId, 10L);

      when(userRepository.existsById(userId)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(memberRepository.existsBySessionIdAndUserId(10L, userId)).thenReturn(false);
      when(memberRepository.countBySessionId(10L)).thenReturn(2, 3); // після join стане 3 = lockedTargetSize
      when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(makeProduct(productId, 10L)));
      when(orderRepository.save(any())).thenReturn(savedOrder);
      when(orderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      // getSessionDetails
      when(productRepository.findById(productId)).thenReturn(Optional.of(makeProduct(productId, 10L)));
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      sessionService.joinSession(session.getUuid(), userId, dto);

      // Перевіряємо що сесія збереглась з COMPLETED
      verify(sessionRepository).save(argThat(s -> "COMPLETED".equals(s.getStatus())));
    }

    @Test
    @DisplayName("кидає BadRequestException якщо сесія не ACTIVE")
    void throws_whenSessionNotActive() {
      GroupBuySession session = makeSession(10L, 100L, "COMPLETED");

      when(userRepository.existsById(2L)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));

      assertThatThrownBy(() -> sessionService.joinSession(session.getUuid(), 2L, makeDto(100L)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("неактивна");
    }

    @Test
    @DisplayName("кидає BadRequestException якщо юзер вже учасник")
    void throws_whenUserAlreadyMember() {
      GroupBuySession session = makeSession(10L, 100L, "ACTIVE");

      when(userRepository.existsById(1L)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(memberRepository.existsBySessionIdAndUserId(10L, 1L)).thenReturn(true);

      assertThatThrownBy(() -> sessionService.joinSession(session.getUuid(), 1L, makeDto(100L)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("вже учасник");
    }

    @Test
    @DisplayName("кидає BadRequestException якщо група заповнена")
    void throws_whenGroupFull() {
      GroupBuySession session = makeSession(10L, 100L, "ACTIVE");

      when(userRepository.existsById(2L)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(memberRepository.existsBySessionIdAndUserId(10L, 2L)).thenReturn(false);
      when(memberRepository.countBySessionId(10L)).thenReturn(3); // вже 3/3

      assertThatThrownBy(() -> sessionService.joinSession(session.getUuid(), 2L, makeDto(100L)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("немає вільних місць");
    }

    @Test
    @DisplayName("кидає BadRequestException якщо productId в dto не збігається з сесією")
    void throws_whenProductMismatch() {
      GroupBuySession session = makeSession(10L, 100L, "ACTIVE");
      GroupBuyCheckoutRequestDto dto = makeDto(999L); // інший продукт

      when(userRepository.existsById(2L)).thenReturn(true);
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(memberRepository.existsBySessionIdAndUserId(10L, 2L)).thenReturn(false);
      when(memberRepository.countBySessionId(10L)).thenReturn(1);

      assertThatThrownBy(() -> sessionService.joinSession(session.getUuid(), 2L, dto))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("не збігається з товаром сесії");
    }
  }

  // ─── getUserSessions ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getUserSessions()")
  class GetUserSessions {

    @Test
    @DisplayName("повертає сесії з фільтром статусу")
    void returnsFilteredSessions() {
      Long userId = 1L;
      GroupBuySession session = makeSession(10L, 100L, "COMPLETED");
      Product product = makeProduct(100L, 10L);

      when(sessionRepository.findAllByUserIdAndStatus(userId, "COMPLETED")).thenReturn(List.of(session));
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(memberRepository.countBySessionId(10L)).thenReturn(3);
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      List<GroupBuySessionResponseDto> result = sessionService.getUserSessions(userId, "COMPLETED");

      assertThat(result).hasSize(1);
      assertThat(result.get(0).status()).isEqualTo("COMPLETED");
      verify(sessionRepository).findAllByUserIdAndStatus(userId, "COMPLETED");
      verify(sessionRepository, never()).findAllByUserId(any());
    }

    @Test
    @DisplayName("повертає всі сесії без фільтру")
    void returnsAllSessions_whenNoStatusFilter() {
      Long userId = 1L;
      GroupBuySession session = makeSession(10L, 100L, "ACTIVE");
      Product product = makeProduct(100L, 10L);

      when(sessionRepository.findAllByUserId(userId)).thenReturn(List.of(session));
      when(sessionRepository.findByUuid(session.getUuid())).thenReturn(Optional.of(session));
      when(productRepository.findById(100L)).thenReturn(Optional.of(product));
      when(memberRepository.countBySessionId(10L)).thenReturn(1);
      when(memberRepository.findBySessionId(10L)).thenReturn(List.of());

      List<GroupBuySessionResponseDto> result = sessionService.getUserSessions(userId, null);

      assertThat(result).hasSize(1);
      verify(sessionRepository).findAllByUserId(userId);
      verify(sessionRepository, never()).findAllByUserIdAndStatus(any(), any());
    }
  }
}
