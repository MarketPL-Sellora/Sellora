package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequestDto(
  @NotBlank(message = "Email не може бути порожнім")
  @Email(message = "Некоректний формат email")
  String email,

  @URL(message = "Avatar URL має бути валідною адресою")
  String avatarUrl // Може бути null, @URL валідує тільки якщо рядок не порожній
) {}
