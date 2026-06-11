package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  String categoryName,
  Long merchantId,
  String storeName,
  Map<String, Object> attributes,
  List<String> images,
  String status,
  boolean isFavorite,
  @JsonProperty("user_active_session_uuid")
  String userActiveSessionUuid
) {}
