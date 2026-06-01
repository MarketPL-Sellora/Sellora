package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartQuantityDto(
  @NotNull(message = "Кількість обов'язкова")
  @Min(value = 1, message = "Мінімум 1")
  @JsonProperty("new_quantity") // Згідно з ТЗ, фронт передає саме таке поле
  Integer newQuantity
) {}
