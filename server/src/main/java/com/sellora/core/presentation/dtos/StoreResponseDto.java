package com.sellora.core.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreResponseDto(
  Long id,
  Long ownerId,
  String name,
  String slug,
  String address,
  String contactPhone,
  String description,
  String logoUrl,
  BigDecimal rating,
  String status,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
