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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuySessionService {

  private final GroupBuySessionRepository sessionRepository;
  private final GroupMemberRepository memberRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PromoCodeRepository promoCodeRepository;
  private final PromoUsageHistoryRepository promoUsageHistoryRepository;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  public GroupBuySessionResponseDto getSessionDetails(String uuid) {
    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію не знайдено"));
    Product product = productRepository.findById(session.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    int currentMembers = memberRepository.countBySessionId(session.getId());
    boolean isAvailable = "ACTIVE".equals(session.getStatus()) && currentMembers < session.getLockedTargetSize();
    String mainImage = (product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null;
    List<GroupMemberDto> memberDtos = memberRepository.findBySessionId(session.getId()).stream()
      .map(m -> new GroupMemberDto(m.getId(), m.getSessionId(), m.getUserId(), m.getJoinedAt())).collect(Collectors.toList());

    return new GroupBuySessionResponseDto(
      session.getUuid(), product.getId(), product.getTitle(), mainImage, session.getLockedPrice(),
      session.getLockedTargetSize(), currentMembers, session.getStatus(), session.getExpiresAt(),
      isAvailable, LocalDateTime.now(), memberDtos, null
    );
  }

  @Transactional
  public GroupBuySessionResponseDto createSession(GroupBuyCheckoutRequestDto dto, Long initiatorId) {
    if (!userRepository.existsById(initiatorId)) throw new ResourceNotFoundException("Користувача не знайдено");

    if (memberRepository.isUserInActiveSessionForProduct(initiatorId, dto.getProductId())) {
      throw new BadRequestException("Ви вже берете участь в активній сесії для цього товару.");
    }

    Product product = productRepository.findById(dto.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    GroupBuySession session = new GroupBuySession();
    session.setUuid(UUID.randomUUID().toString());
    session.setProductId(product.getId());
    session.setInitiatorId(initiatorId);
    session.setStatus("ACTIVE");
    session.setExpiresAt(LocalDateTime.now().plusHours(24));
    session.setLockedPrice(product.getGroupPrice() != null ? product.getGroupPrice() : product.getStandardPrice());
    session.setLockedTargetSize(product.getGroupTargetSize() != null ? product.getGroupTargetSize() : 3);

    GroupBuySession savedSession = sessionRepository.save(session);
    return processGroupBuyOrder(initiatorId, dto, savedSession);
  }

  @Transactional
  public GroupBuySessionResponseDto joinSession(String uuid, Long userId, GroupBuyCheckoutRequestDto dto) {
    if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("Користувача не знайдено");

    GroupBuySession session = sessionRepository.findByUuid(uuid)
      .orElseThrow(() -> new ResourceNotFoundException("Сесію не знайдено"));

    if (!"ACTIVE".equals(session.getStatus())) throw new BadRequestException("Ця сесія вже неактивна");
    if (memberRepository.existsBySessionIdAndUserId(session.getId(), userId)) throw new BadRequestException("Ви вже учасник цієї групи");
    if (memberRepository.countBySessionId(session.getId()) >= session.getLockedTargetSize()) throw new BadRequestException("В групі немає вільних місць");
    if (!session.getProductId().equals(dto.getProductId())) throw new BadRequestException("Товар у запиті не збігається з товаром сесії");

    return processGroupBuyOrder(userId, dto, session);
  }

  public List<GroupBuySessionResponseDto> getUserSessions(Long userId, String status) {
    List<GroupBuySession> sessions = (status != null && !status.trim().isEmpty())
      ? sessionRepository.findAllByUserIdAndStatus(userId, status.toUpperCase())
      : sessionRepository.findAllByUserId(userId);

    return sessions.stream().map(s -> getSessionDetails(s.getUuid())).collect(Collectors.toList());
  }

  private GroupBuySessionResponseDto processGroupBuyOrder(Long userId, GroupBuyCheckoutRequestDto request, GroupBuySession session) {
    // 1. Валідація доставки
    String deliveryType = request.getDeliveryType().toUpperCase();
    if (("BRANCH".equals(deliveryType) || "COURIER".equals(deliveryType)) && (request.getCarrierId() == null || request.getDeliveryAddress() == null)) {
      throw new BadRequestException("Для доставки BRANCH або COURIER обов'язково вказати carrier_id та delivery_address");
    }

    // 2. Блокування товару та перевірка стоку
    Product product = productRepository.findByIdForUpdate(request.getProductId())
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено або він не активний"));

    if (product.getStockQuantity() < request.getQuantity()) {
      throw new ConflictException("Недостатньо товару на складі. Доступно: " + product.getStockQuantity());
    }

    // 3. Розрахунок цін
    BigDecimal subtotal = session.getLockedPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
    BigDecimal discount = BigDecimal.ZERO;
    PromoCode appliedPromo = null;

    if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
      appliedPromo = promoCodeRepository.findByCode(request.getPromoCode()).orElseThrow(() -> new ResourceNotFoundException("Промокод не знайдено"));
      if (!appliedPromo.getIsActive()) throw new ConflictException("Промокод неактивний");
      OffsetDateTime now = OffsetDateTime.now();
      if (appliedPromo.getStartDate() != null && now.isBefore(appliedPromo.getStartDate())) throw new ConflictException("Дія промокоду ще не почалась");
      if (appliedPromo.getEndDate() != null && now.isAfter(appliedPromo.getEndDate())) throw new ConflictException("Дія промокоду завершилась");
      if (appliedPromo.getUsageLimit() != null && appliedPromo.getUsedCount() >= appliedPromo.getUsageLimit()) throw new ConflictException("Ліміт використання вичерпано");
      if (promoUsageHistoryRepository.existsByUserIdAndPromoId(userId, appliedPromo.getId())) throw new ConflictException("Промокод вже використано вами");

      discount = "PERCENTAGE".equalsIgnoreCase(appliedPromo.getDiscountType()) ?
        subtotal.multiply(appliedPromo.getValue()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP) : appliedPromo.getValue();
      if (discount.compareTo(subtotal) > 0) discount = subtotal;

      appliedPromo.setUsedCount(appliedPromo.getUsedCount() + 1);
      promoCodeRepository.save(appliedPromo);
    }

    BigDecimal tax = subtotal.subtract(discount).multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalAmount = subtotal.subtract(discount).add(tax);

    // 4. Списання залишків
    product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
    if (product.getStockQuantity() == 0) product.setStatus("OUT_OF_STOCK");
    productRepository.save(product);

    // 5. Створення DRAFT замовлення
    String paymentMethod = request.getPaymentMethod().toUpperCase();
    Order order = new Order();
    order.setUserId(userId);
    order.setMerchantId(product.getMerchantId());
    order.setSessionId(session.getId());
    order.setPurchaseType("GROUP_BUY");
    order.setBuyerName(request.getBuyerName());
    order.setBuyerSurname(request.getBuyerSurname());
    order.setBuyerPhone(request.getBuyerPhone());
    order.setBuyerEmail(request.getBuyerEmail());
    order.setDeliveryType(deliveryType);
    order.setCarrierId(request.getCarrierId());
    try { order.setDeliveryAddress(request.getDeliveryAddress() != null ? objectMapper.writeValueAsString(request.getDeliveryAddress()) : null); }
    catch (JsonProcessingException e) { throw new BadRequestException("Помилка формату delivery_address"); }
    order.setPaymentMethod(paymentMethod);
    order.setOrderComment(request.getOrderComment());
    order.setSubtotal(subtotal);
    order.setDiscount(discount);
    order.setTax(tax);
    order.setFinalPrice(totalAmount);
    order.setPaymentStatus("ONLINE_CARD".equals(paymentMethod) ? "PENDING" : "WAITING_FOR_GROUP");
    order.setShippingStatus("PENDING");
    Order savedOrder = orderRepository.save(order);

    OrderItem oi = new OrderItem();
    oi.setOrderId(savedOrder.getId());
    oi.setProductId(product.getId());
    oi.setQuantity(request.getQuantity());
    oi.setPriceSnapshot(session.getLockedPrice());
    oi.setTitleSnapshot(product.getTitle());
    oi.setImageSnapshot((product.getImages() != null && !product.getImages().isEmpty()) ? product.getImages().get(0) : null);
    orderItemRepository.save(oi);

    if (appliedPromo != null) {
      PromoUsageHistory usage = new PromoUsageHistory();
      usage.setPromoId(appliedPromo.getId());
      usage.setUserId(userId);
      usage.setOrderId(savedOrder.getId());
      promoUsageHistoryRepository.save(usage);
    }

    // 6. Додавання в групу (ТІЛЬКИ ДЛЯ ГОТІВКИ)
    if ("CASH_ON_DELIVERY".equals(paymentMethod)) {
      GroupMember member = new GroupMember();
      member.setSessionId(session.getId());
      member.setUserId(userId);
      member.setJoinedAt(LocalDateTime.now());
      memberRepository.save(member);

      if (memberRepository.countBySessionId(session.getId()) >= session.getLockedTargetSize()) {
        session.setStatus("COMPLETED");
        sessionRepository.save(session);
      }
    }

    // 7. Повернення відповіді
    String paymentUrl = "ONLINE_CARD".equals(paymentMethod) ? paymentService.generatePaymentUrl(savedOrder.getId(), totalAmount) : null;
    GroupBuySessionResponseDto d = getSessionDetails(session.getUuid());

    return new GroupBuySessionResponseDto(d.uuid(), d.productId(), d.productTitle(), d.productImage(), d.price(),
      d.targetSize(), d.currentMembersCount(), d.status(), d.expiresAt(),
      d.isAvailable(), d.serverTime(), d.members(), paymentUrl);
  }
}
