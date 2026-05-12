package com.sellora.core.presentation.dtos;

import java.time.LocalDateTime;

public record CategoryResponseDto(
  Long id,
  String name,
  Long parentId,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
