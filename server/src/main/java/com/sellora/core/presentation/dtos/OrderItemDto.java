package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderItemDto(
  Long id,

  @JsonProperty("product_id")
  Long productId,

  Integer quantity,

  @JsonProperty("unit_price")
  BigDecimal unitPrice,

  @JsonProperty("total_price")
  BigDecimal totalPrice,

  @JsonProperty("title_snapshot")
  String titleSnapshot,

  @JsonProperty("image_snapshot")
  String imageSnapshot
) {}
