package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDto(
  @NotBlank(message = "Старий пароль є обов'язковим")
  String oldPassword,

  @NotBlank(message = "Новий пароль є обов'язковим")
  String newPassword
) {}
