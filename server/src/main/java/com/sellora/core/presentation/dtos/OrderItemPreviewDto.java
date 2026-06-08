package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItemPreviewDto(
  @JsonProperty("image_snapshot") String imageSnapshot,
  @JsonProperty("title_snapshot") String titleSnapshot
) {}
