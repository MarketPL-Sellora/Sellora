package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record ReviewWithUserDto(
  Long id,
  @JsonProperty("user_id") Long userId,
  String email,
  Integer rating,
  String comment,
  @JsonProperty("created_at") OffsetDateTime createdAt
) {}
