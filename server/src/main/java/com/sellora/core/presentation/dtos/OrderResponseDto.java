package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponseDto(
  Long id,
  Long userId,
  Long merchantId,
  BigDecimal finalPrice,
  String paymentStatus,
  String shippingStatus,
  String purchaseType,
  List<OrderItemDto> items
) {}
