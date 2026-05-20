package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailsResponseDto(
  Long id,
  String purchaseType,
  Long merchantId,
  BigDecimal finalPrice,
  String paymentStatus,
  String shippingStatus,
  LocalDateTime createdAt,
  List<OrderItemDto> items // <--- Додаємо масив куплених товарів
) {}
