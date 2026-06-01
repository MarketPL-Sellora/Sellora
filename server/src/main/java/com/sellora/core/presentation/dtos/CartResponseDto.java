package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDto(
  List<CartItemResponseDto> items,
  BigDecimal totalAmount
) {}
