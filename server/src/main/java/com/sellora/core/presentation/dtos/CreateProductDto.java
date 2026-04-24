package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateProductDto(
  // --- ОСНОВНА ІНФОРМАЦІЯ ---
  @NotBlank(message = "Назва товару не може бути порожньою")
  @Size(max = 255, message = "Назва не може перевищувати 255 символів")
  String title,

  String description,

  // --- ЗВ'ЯЗКИ (Категорія та Магазин) ---
  @NotNull(message = "Категорія є обов'язковою")
  Long categoryId,

  // В ідеалі storeId ми беремо з токена (SecurityContext) авторизованого продавця,
  // але якщо Вадим передає його з фронта, розблокуй це поле:
  // @NotNull(message = "ID магазину є обов'язковим")
  // Long storeId,

  // --- ЦІНИ (DB: standard_price >= 0 AND group_price >= 0) ---
  @NotNull(message = "Стандартна ціна є обов'язковою")
  @Min(value = 0, message = "Стандартна ціна не може бути від'ємною")
  BigDecimal standardPrice,

  @NotNull(message = "Групова ціна є обов'язковою")
  @Min(value = 0, message = "Групова ціна не може бути від'ємною")
  BigDecimal groupPrice,

  // --- КІЛЬКІСТЬ (DB: group_target_size > 1, stock_quantity >= 0) ---
  @NotNull(message = "Розмір групи є обов'язковим")
  @Min(value = 2, message = "Для групової покупки потрібно мінімум 2 людини")
  Integer groupTargetSize,

  @NotNull(message = "Кількість на складі є обов'язковою")
  @Min(value = 0, message = "Кількість не може бути від'ємною")
  Integer stockQuantity,

  // --- МЕДІА ТА ХАРАКТЕРИСТИКИ (DB: JSONB) ---
  // Список URL посилань на картинки з Cloudinary
  List<String> images,

  // Динамічні характеристики у форматі Ключ-Значення
  // (наприклад: {"Колір": "Чорний", "Пам'ять": "256GB"})
  Map<String, Object> attributes
) {}
