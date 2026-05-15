package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateProductDto(

  @NotBlank(message = "Назва товару не може бути порожньою")
  @Size(max = 255, message = "Назва не може перевищувати 255 символів")
  String title,

  String description,

  @NotNull(message = "Категорія є обов'язковою")
  Long categoryId,

  @NotNull(message = "Стандартна ціна є обов'язковою")
  @Min(value = 0, message = "Стандартна ціна не може бути від'ємною")
  BigDecimal standardPrice,

  @Min(value = 0, message = "Групова ціна не може бути від'ємною")
  BigDecimal groupPrice,

  @NotNull(message = "Розмір групи є обов'язковим")
  @Min(value = 2, message = "Для групової покупки потрібно мінімум 2 людини")
  @Max(value = 5, message = "Максимальна кількість учасників групової покупки не може перевищувати 5")
  Integer groupTargetSize,

  @NotNull(message = "Кількість на складі є обов'язковою")
  @Min(value = 0, message = "Кількість не може бути від'ємною")
  Integer stockQuantity,

  List<String> images,

  Map<String, Object> attributes
) {}
