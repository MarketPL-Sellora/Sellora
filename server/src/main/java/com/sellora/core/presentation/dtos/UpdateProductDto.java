package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateProductDto(
  @NotBlank(message = "Назва не може бути порожньою")
  String title,

  String description,

  @NotNull(message = "Категорія обов'язкова")
  Long categoryId,

  @NotNull(message = "Стандартна ціна обов'язкова")
  @Min(value = 0, message = "Ціна не може бути від'ємною")
  BigDecimal standardPrice,

  BigDecimal groupPrice,

  Integer groupTargetSize,

  @NotNull(message = "Кількість на складі обов'язкова")
  @Min(value = 0, message = "Кількість не може бути від'ємною")
  Integer stockQuantity,

  List<String> images,

  Map<String, Object> attributes
) {}
