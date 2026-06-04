package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.application.usecases.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final ObjectMapper objectMapper; // Для парсингу JSON від LiqPay

  @Value("${liqpay.private-key}")
  private String privateKey;

  // 1. Ендпоінт для отримання вебхука від LiqPay
  @PostMapping(value = "/webhook", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<String> liqpayWebhook(
    @RequestParam("data") String data,
    @RequestParam("signature") String signature) {

    try {
      // Крок А: Перевірка підпису (щоб хакери не підробили оплату)
      String signString = privateKey + data + privateKey;
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] hash = md.digest(signString.getBytes("UTF-8"));
      String expectedSignature = Base64.getEncoder().encodeToString(hash);

      if (!expectedSignature.equals(signature)) {
        return ResponseEntity.badRequest().body("Invalid signature");
      }

      // Крок Б: Розшифровуємо data (це Base64 закодований JSON)
      String decodedData = new String(Base64.getDecoder().decode(data));
      JsonNode jsonNode = objectMapper.readTree(decodedData);

      // Крок В: Витягуємо потрібні поля
      String status = jsonNode.get("status").asText();
      Long orderId = Long.parseLong(jsonNode.get("order_id").asText());
      java.math.BigDecimal amount = new java.math.BigDecimal(jsonNode.get("amount").asText());
      String paymentId = jsonNode.get("payment_id").asText(); // Це наш idempotencyKey

      // Крок Г: Передаємо в наш Сервіс (який ми написали раніше)
      paymentService.processLiqPayWebhook(paymentId, orderId, status, amount);

      return ResponseEntity.ok("OK");

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().body("Error processing webhook");
    }
  }
  @Value("${liqpay.public-key}")
  private String publicKey;

  // Тимчасовий ендпоінт для тестування генерації оплати
  @GetMapping("/test-pay/{orderId}")
  public String generateTestPaymentHtml(@PathVariable Long orderId) {
    try {
      java.util.HashMap<String, String> params = new java.util.HashMap<>();
      params.put("action", "pay");
      params.put("amount", "100.00"); // Сума для тесту
      params.put("currency", "UAH");
      params.put("description", "Оплата замовлення №" + orderId);
      params.put("order_id", String.valueOf(orderId));
      params.put("version", "3");
      params.put("sandbox", "1"); // ВАЖЛИВО: Режим пісочниці (тестові гроші)

      params.put("server_url", "https://bartender-payphone-sizzle.ngrok-free.dev");

      com.liqpay.LiqPay liqpay = new com.liqpay.LiqPay(publicKey, privateKey);
      return liqpay.cnb_form(params); // Повертає готову HTML-кнопку
    } catch (Exception e) {
      return "Помилка: " + e.getMessage();
    }
  }
}
