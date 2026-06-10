package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderCancelResponseDto(
  Long id,
  @JsonProperty("payment_status") String paymentStatus,
  @JsonProperty("shipping_status") String shippingStatus,
  String message
) {}
