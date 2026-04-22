package com.sellora.core.presentation.dtos;

import lombok.Data;

@Data
public class RegisterRequest {
  private String email;
  private String password;
}
