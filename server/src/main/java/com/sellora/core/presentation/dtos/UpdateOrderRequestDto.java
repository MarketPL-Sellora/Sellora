package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateOrderRequestDto(
  @JsonProperty("payment_status") String paymentStatus,
  @JsonProperty("shipping_status") String shippingStatus,
  @JsonProperty("tracking_number") String trackingNumber
) {}
