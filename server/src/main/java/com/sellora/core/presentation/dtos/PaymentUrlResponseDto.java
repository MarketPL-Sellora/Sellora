package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentUrlResponseDto(
  @JsonProperty("payment_url") String paymentUrl
) {}
