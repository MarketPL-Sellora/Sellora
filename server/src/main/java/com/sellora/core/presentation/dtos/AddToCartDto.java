package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartDto(
  @NotNull(message = "ID товару обов'язковий")
  Long productId,

  @Min(value = 1, message = "Кількість має бути мінімум 1")
  Integer quantity
) {}
