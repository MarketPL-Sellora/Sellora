package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStoreRequest(
  @NotBlank(message = "Назва магазину не може бути порожньою")
  String name,

  String address,

  @NotBlank(message = "Номер телефону обов'язковий")
  @Pattern(regexp = "^\\+?\\d{6,15}$", message = "Некоректний формат телефону. Має містити щонайменше 6 цифр (може починатися з +)")
  String contactPhone,

  String description,

  String logoUrl
) {}
