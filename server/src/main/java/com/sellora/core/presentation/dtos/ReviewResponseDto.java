package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record ReviewResponseDto(
  Long id,
  @JsonProperty("product_id") Long productId,
  @JsonProperty("user_id") Long userId,
  Integer rating,
  String comment,
  @JsonProperty("created_at") OffsetDateTime createdAt,
  @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
