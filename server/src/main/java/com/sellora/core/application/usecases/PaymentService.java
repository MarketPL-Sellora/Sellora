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

    // 1. Атомарно записуємо в INSERT ONLY таблицю історії
    TransactionEvent event = new TransactionEvent();
    event.setIdempotencyKey(idempotencyKey); // LiqPay передає свій order_id або payment_id
    event.setOrderId(orderId);
    event.setEventType(status);
    event.setAmount(amount);
    transactionRepository.save(event);

    // 2. Атомарно оновлюємо статус в таблиці Orders
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new RuntimeException("Замовлення не знайдено"));

    // Мапимо статуси LiqPay на наші
    if ("success".equalsIgnoreCase(status) || "wait_accept".equalsIgnoreCase(status)) {
      order.setPaymentStatus("PAID");
    } else if ("error".equalsIgnoreCase(status) || "failure".equalsIgnoreCase(status)) {
      order.setPaymentStatus("FAILED");
    } else if ("refund".equalsIgnoreCase(status)) {
      order.setPaymentStatus("REFUNDED");
    }

    orderRepository.save(order);
  }
}
