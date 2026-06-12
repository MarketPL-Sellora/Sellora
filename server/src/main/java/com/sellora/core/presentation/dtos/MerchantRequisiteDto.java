package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public record MerchantRequisiteDto(
  Long id,
  @JsonProperty("owner_id") Long ownerId,

  @NotBlank(message = "ЄДРПОУ не може бути порожнім")
  @Pattern(regexp = "^\\d{8,10}$", message = "ЄДРПОУ має містити від 8 до 10 цифр")
  String edrpou,

  @NotBlank(message = "IBAN не може бути порожнім")
  @Pattern(regexp = "^UA\\d{27}$", message = "Некоректний формат IBAN (має починатися з 'UA' та містити 27 цифр)")
  String iban,

  @NotBlank(message = "Назва банку обов'язкова")
  @JsonProperty("bank_name") String bankName,

  @NotNull @JsonProperty("is_primary") Boolean isPrimary,

  @JsonProperty("created_at") OffsetDateTime createdAt
) {}
