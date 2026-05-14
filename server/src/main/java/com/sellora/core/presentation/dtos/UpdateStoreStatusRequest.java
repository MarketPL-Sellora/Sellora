package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStoreStatusRequest(
  @NotBlank(message = "Статус обов'язковий")
  @Pattern(regexp = "^(PENDING|ACTIVE|BLOCKED|CLOSED)$", message = "Доступні статуси: PENDING, ACTIVE, BLOCKED, CLOSED")
  String status
) {}
