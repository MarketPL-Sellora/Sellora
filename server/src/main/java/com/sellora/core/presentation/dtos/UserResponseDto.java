package com.sellora.core.presentation.dtos;

import java.time.LocalDateTime;

public record UserResponseDto(
  Long id,
  String email,
  String role,
  String avatarUrl,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
