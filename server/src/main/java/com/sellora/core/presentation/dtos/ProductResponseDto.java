package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductResponseDto(
  Long id,
  String title,
  String description,
  BigDecimal standardPrice,
  BigDecimal groupPrice,
  Integer groupTargetSize,
  Integer stockQuantity,
  Long categoryId,
  String categoryName, // Заповнюється, якщо full=true
  Long merchantId,
  String storeName,    // Заповнюється, якщо full=true
  Map<String, Object> attributes,
  List<String> images,
  String status,
  boolean isFavorite
) {}
