package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductDto(
  @NotBlank(message = "Назва товару не може бути порожньою")
  String title,

  @NotNull(message = "Ціна є обов'язковою")
  @Min(value = 1, message = "Ціна має бути більшою за 0")
  BigDecimal standardPrice,

  @Min(value = 0, message = "Кількість не може бути від'ємною")
  Integer stockQuantity
) {}
