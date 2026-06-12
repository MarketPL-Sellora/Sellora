package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequestDto(
  @NotNull(message = "Рейтинг є обов'язковим")
  @Min(value = 1, message = "Мінімальна оцінка 1")
  @Max(value = 5, message = "Максимальна оцінка 5")
  Integer rating,

  String comment
) {}
