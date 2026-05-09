package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GroupBuySessionResponseDto(
  String uuid,
  Long productId,
  String productTitle,
  String productImage,
  BigDecimal price,
  Integer targetSize,
  Integer currentMembersCount,
  String status,
  LocalDateTime expiresAt,
  boolean isAvailable,// true, якщо сесія ACTIVE і ще є вільні місця
  LocalDateTime serverTime
) {}
