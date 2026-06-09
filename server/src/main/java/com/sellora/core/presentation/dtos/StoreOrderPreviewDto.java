package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StoreOrderPreviewDto(
  Long id,
  @JsonProperty("purchase_type") String purchaseType,

  @JsonProperty("buyer_name") String buyerName,
  @JsonProperty("buyer_surname") String buyerSurname,
  @JsonProperty("buyer_phone") String buyerPhone,

  @JsonProperty("total_amount") BigDecimal totalAmount,
  @JsonProperty("payment_status") String paymentStatus,
  @JsonProperty("shipping_status") String shippingStatus,
  @JsonProperty("created_at") LocalDateTime createdAt,
  @JsonProperty("items_preview") List<OrderItemPreviewDto> itemsPreview
) {}
