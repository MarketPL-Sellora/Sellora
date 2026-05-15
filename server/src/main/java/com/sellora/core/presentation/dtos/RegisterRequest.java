package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

  @NotBlank(message = "Email не може бути порожнім")
  @Email(message = "Некоректний формат email адреси")
  private String email;

  @NotBlank(message = "Пароль не може бути порожнім")
  @Size(min = 6, message = "Пароль має містити щонайменше 6 символів")
  private String password;
}
