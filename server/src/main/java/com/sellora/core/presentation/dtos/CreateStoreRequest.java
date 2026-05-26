package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateStoreRequest {
  @NotBlank(message = "Назва магазину не може бути порожньою")
  private String name;

  @NotBlank(message = "Адреса не може бути порожньою")
  private String address;

  @NotBlank(message = "Номер телефону обов'язковий")
  @Pattern(regexp = "^\\+?\\d{6,15}$", message = "Некоректний формат телефону. Має містити щонайменше 6 цифр (може починатися з +)")
  private String contactPhone;

  private String description;

  // Фронтенд передасть сюди URL, який отримав з UploadController
  private String logoUrl;
}
