package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
  LocalDateTime updatedAt,
  @JsonProperty("shipping_methods") List<StoreShippingMethodDto> shippingMethods
) {}
