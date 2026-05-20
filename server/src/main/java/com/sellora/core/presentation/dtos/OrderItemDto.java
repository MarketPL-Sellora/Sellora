package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;

public record OrderItemDto(
  Long productId,
  String title,
  String image,
  Integer quantity,
  BigDecimal price
) {}
