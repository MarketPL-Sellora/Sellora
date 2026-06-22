package com.sellora.core.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.domain.entities.GroupMember;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.TransactionEvent;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import com.sellora.core.infrastructure.persistence.TransactionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.net.URLEncoder;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final TransactionEventRepository transactionRepository;
  private final OrderRepository orderRepository;
  private final GroupBuySessionRepository groupBuySessionRepository;
  private final GroupMemberRepository groupMemberRepository;

  @Value("${liqpay.public-key}")
  private String publicKey;

  @Value("${liqpay.private-key}")
  private String privateKey;

  // --- ДОДАНО ---
  @Value("${liqpay.webhook-url}")
  private String webhookUrl;

  @Transactional
  public void processLiqPayWebhook(String idempotencyKey, Long orderId, String status, BigDecimal amount) {

    String internalEventType = "PENDING";
    String orderPaymentStatus = "PENDING";

    if ("success".equalsIgnoreCase(status) || "wait_accept".equalsIgnoreCase(status)
        || "sandbox".equalsIgnoreCase(status)) {
      internalEventType = "PAYMENT";
      orderPaymentStatus = "PAID";
    } else if ("error".equalsIgnoreCase(status) || "failure".equalsIgnoreCase(status)) {
      internalEventType = "FAILED";
      orderPaymentStatus = "FAILED";
    } else if ("refund".equalsIgnoreCase(status)) {
      internalEventType = "REFUND";
      orderPaymentStatus = "REFUNDED";
    }

    TransactionEvent event = new TransactionEvent();
    event.setIdempotencyKey(idempotencyKey);
    event.setOrderId(orderId);
    event.setEventType(internalEventType);
    event.setAmount(amount);
    transactionRepository.save(event);

    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Замовлення не знайдено"));

    order.setPaymentStatus(orderPaymentStatus);
    orderRepository.save(order);

    // --- ЛОГІКА ДЛЯ ГРУПОВИХ ПОКУПОК ---
    if ("GROUP_BUY".equals(order.getPurchaseType()) && "PAID".equals(orderPaymentStatus)) {
      if (!groupMemberRepository.existsBySessionIdAndUserId(order.getSessionId(), order.getUserId())) {

        GroupMember member = new GroupMember();
        member.setSessionId(order.getSessionId());
        member.setUserId(order.getUserId());
        member.setJoinedAt(LocalDateTime.now());
        groupMemberRepository.save(member);

        GroupBuySession session = groupBuySessionRepository.findById(order.getSessionId()).orElseThrow();
        int currentMembers = groupMemberRepository.countBySessionId(session.getId());

        if (currentMembers >= session.getLockedTargetSize()) {
          session.setStatus("COMPLETED");
          groupBuySessionRepository.save(session);

          List<Order> sessionOrders = orderRepository.findBySessionId(session.getId());
          for (Order o : sessionOrders) {
            if ("WAITING_FOR_GROUP".equals(o.getPaymentStatus())) {
              o.setPaymentStatus("PENDING");
            }
          }
          orderRepository.saveAll(sessionOrders);
        }
      }
    }
  }

  public String generatePaymentUrl(Long orderId, BigDecimal amount) {
    try {
      HashMap<String, String> params = new HashMap<>();
      params.put("public_key", publicKey);
      params.put("action", "pay");
      params.put("amount", amount.toString());
      params.put("currency", "UAH");
      params.put("description", "Оплата замовлення №" + orderId);
      params.put("order_id", String.valueOf(orderId));
      params.put("version", "3");
      params.put("sandbox", "1");

      // --- ЗМІНЕНО ---
      params.put("server_url", webhookUrl);

      String data = Base64.getEncoder().encodeToString(
          new ObjectMapper().writeValueAsString(params).getBytes(StandardCharsets.UTF_8));
      String signString = privateKey + data + privateKey;
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      String signature = Base64.getEncoder().encodeToString(md.digest(signString.getBytes(StandardCharsets.UTF_8)));

      // URL-кодування рядків
      String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8.name());
      String encodedSignature = URLEncoder.encode(signature, StandardCharsets.UTF_8.name());

      return "https://www.liqpay.ua/api/3/checkout?data=" + encodedData + "&signature=" + encodedSignature;
    } catch (Exception e) {
      throw new RuntimeException("Помилка генерації посилання LiqPay", e);
    }
  }
}