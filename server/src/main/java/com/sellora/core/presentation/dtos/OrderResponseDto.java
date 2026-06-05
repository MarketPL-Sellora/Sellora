package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
  Long id,

  @JsonProperty("purchase_type") String purchaseType,
  @JsonProperty("session_id") Long sessionId,
  @JsonProperty("user_id") Long userId,
  @JsonProperty("store_id") Long storeId,

  @JsonProperty("buyer_name") String buyerName,
  @JsonProperty("buyer_surname") String buyerSurname,
  @JsonProperty("buyer_phone") String buyerPhone,
  @JsonProperty("buyer_email") String buyerEmail,

  @JsonProperty("delivery_type") String deliveryType,
  @JsonProperty("carrier_id") Long carrierId,

  // Використовуємо Object, щоб JSONB розпарсився як справжній JSON-об'єкт, а не строка
  @JsonProperty("delivery_address") Object deliveryAddress,

  @JsonProperty("tracking_number") String trackingNumber,
  @JsonProperty("payment_method") String paymentMethod,
  @JsonProperty("order_comment") String orderComment,

  BigDecimal subtotal,
  BigDecimal tax,
  BigDecimal discount,

  @JsonProperty("total_amount") BigDecimal totalAmount,

  @JsonProperty("payment_status") String paymentStatus,
  @JsonProperty("shipping_status") String shippingStatus,

  @JsonProperty("payment_url") String paymentUrl,

  @JsonProperty("created_at") LocalDateTime createdAt,
  @JsonProperty("updated_at") LocalDateTime updatedAt,

  List<OrderItemDto> items
) {}
