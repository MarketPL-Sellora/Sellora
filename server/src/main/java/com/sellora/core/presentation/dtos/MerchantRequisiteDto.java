package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record MerchantRequisiteDto(
  Long id,
  @JsonProperty("owner_id") Long ownerId,
  @NotBlank String edrpou,
  @NotBlank String iban,
  @NotBlank @JsonProperty("bank_name") String bankName,
  @NotNull @JsonProperty("is_primary") Boolean isPrimary,
  @JsonProperty("created_at") OffsetDateTime createdAt
) {}
