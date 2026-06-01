package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;

public record AddToCartResponseDto(
  Long productId,
  Integer quantity,
  BigDecimal subTotal, // Сума конкретного товару
  BigDecimal totalAmount // Сума всього кошика
) {}
