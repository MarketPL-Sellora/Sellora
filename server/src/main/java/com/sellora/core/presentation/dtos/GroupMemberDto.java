package com.sellora.core.presentation.dtos;

import java.time.LocalDateTime;

public record GroupMemberDto(
  Long id,
  Long sessionId,
  Long userId,
  LocalDateTime joinedAt
) {}
