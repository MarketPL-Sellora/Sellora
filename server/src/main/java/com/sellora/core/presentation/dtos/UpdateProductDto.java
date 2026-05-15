package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateProductDto(

  @NotBlank(message = "Назва не може бути порожньою")
  String title,

  @NotBlank(message = "Опис товару не може бути порожнім")
  @Size(min = 50, message = "Опис товару має містити щонайменше 50 символів")
  String description,

  @NotNull(message = "Категорія обов'язкова")
  Long categoryId,

  @NotNull(message = "Стандартна ціна обов'язкова")
  @Min(value = 0, message = "Ціна не може бути від'ємною")
  BigDecimal standardPrice,

  BigDecimal groupPrice,

  @NotNull(message = "Розмір групи є обов'язковим")
  @Min(value = 2, message = "Для групової покупки потрібно мінімум 2 людини")
  @Max(value = 5, message = "Максимальна кількість учасників групової покупки не може перевищувати 5")
  Integer groupTargetSize,

  @NotNull(message = "Кількість на складі обов'язкова")
  @Min(value = 0, message = "Кількість не може бути від'ємною")
  Integer stockQuantity,

  List<String> images,

  Map<String, Object> attributes
) {}
