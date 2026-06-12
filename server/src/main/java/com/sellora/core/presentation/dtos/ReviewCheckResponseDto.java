package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewCheckResponseDto(
  @JsonProperty("can_review") boolean canReview,
  String reason
) {}
