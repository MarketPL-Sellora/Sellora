package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderPreviewDto(
  Long id,
  @JsonProperty("purchase_type") String purchaseType,
  @JsonProperty("store_id") Long storeId,
  @JsonProperty("total_amount") BigDecimal totalAmount,
  @JsonProperty("payment_status") String paymentStatus,
  @JsonProperty("shipping_status") String shippingStatus,
  @JsonProperty("created_at") LocalDateTime createdAt,
  @JsonProperty("items_preview") List<OrderItemPreviewDto> itemsPreview
) {}
