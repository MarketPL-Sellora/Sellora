package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProductStatusDto(
  @NotBlank(message = "Статус обов'язковий")
  @Pattern(regexp = "^(ACTIVE|ARCHIVED)$", message = "Доступні статуси для зміни: ACTIVE або ARCHIVED")
  String status
) {}
