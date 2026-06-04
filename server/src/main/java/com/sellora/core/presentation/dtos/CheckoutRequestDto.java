package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequestDto {
  @NotBlank(message = "Ім'я покупця обов'язкове")
  private String buyerName;

  private String buyerSurname;

  @NotBlank(message = "Телефон обов'язковий")
  private String buyerPhone;

  // --- НОВІ ПОЛЯ ---
  @NotBlank(message = "Тип доставки обов'язковий")
  private String deliveryType;

  @NotBlank(message = "Метод оплати обов'язковий")
  private String paymentMethod;
}
