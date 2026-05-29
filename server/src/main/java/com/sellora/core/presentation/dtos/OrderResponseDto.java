package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponseDto(
  Long id,
  String purchaseType,
  Long merchantId,
  BigDecimal finalPrice,
  String paymentStatus,
  String shippingStatus,
  LocalDateTime createdAt
) {}
