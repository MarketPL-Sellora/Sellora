package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemDto(
  @NotNull @JsonProperty("product_id") Long productId,
  @NotNull @Min(1) Integer quantity
) {}
