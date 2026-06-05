package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.TransactionEvent;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import com.sellora.core.infrastructure.persistence.TransactionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final TransactionEventRepository transactionRepository;
  private final OrderRepository orderRepository;

  // Дістаємо ключі з application.yml
  @Value("${liqpay.public-key}")
  private String publicKey;

  @Value("${liqpay.private-key}")
  private String privateKey;

  @Transactional
  public void processLiqPayWebhook(String idempotencyKey, Long orderId, String status, BigDecimal amount) {

    String internalEventType = "PENDING";
    String orderPaymentStatus = "PENDING";

    // Змінюємо мапінг для transaction_events на PAYMENT
    if ("success".equalsIgnoreCase(status) || "wait_accept".equalsIgnoreCase(status) || "sandbox".equalsIgnoreCase(status)) {
      internalEventType = "PAYMENT";
      orderPaymentStatus = "PAID";
    } else if ("error".equalsIgnoreCase(status) || "failure".equalsIgnoreCase(status)) {
      internalEventType = "FAILED";
      orderPaymentStatus = "FAILED";
    } else if ("refund".equalsIgnoreCase(status)) {
      internalEventType = "REFUND";
      orderPaymentStatus = "REFUNDED";
    }

    // 2. Записуємо в таблицю історії
    TransactionEvent event = new TransactionEvent();
    event.setIdempotencyKey(idempotencyKey);
    event.setOrderId(orderId);
    event.setEventType(internalEventType);
    event.setAmount(amount);
    transactionRepository.save(event);

    // 3. Оновлюємо статус в таблиці Orders
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new RuntimeException("Замовлення не знайдено"));

    order.setPaymentStatus(orderPaymentStatus);
    orderRepository.save(order);
  }

  public String generatePaymentUrl(Long orderId, BigDecimal amount) {
    try {
      java.util.HashMap<String, String> params = new java.util.HashMap<>();

      params.put("public_key", publicKey);
      params.put("action", "pay");
      params.put("amount", amount.toString());
      params.put("currency", "UAH");
      params.put("description", "Оплата замовлення №" + orderId);
      params.put("order_id", String.valueOf(orderId));
      params.put("version", "3");

      // --- ДОДАНО ДЛЯ ТЕСТУВАННЯ ---
      params.put("sandbox", "1"); // Вмикає режим тестування (гроші не списуються)

      // Замінюємо на актуальний Ngrok URL:
      params.put("server_url", "https://bartender-payphone-sizzle.ngrok-free.dev/api/v1/payments/webhook");

      // Кодуємо дані для URL згідно документації LiqPay
      String data = java.util.Base64.getEncoder().encodeToString(
        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params).getBytes(java.nio.charset.StandardCharsets.UTF_8)
      );
      String signString = privateKey + data + privateKey;
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
      String signature = java.util.Base64.getEncoder().encodeToString(md.digest(signString.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

      return "https://www.liqpay.ua/api/3/checkout?data=" + data + "&signature=" + signature;
    } catch (Exception e) {
      throw new RuntimeException("Помилка генерації посилання LiqPay", e);
    }
  }
}
