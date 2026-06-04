package com.sellora.core.presentation.dtos;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShippingCarrierDto(
  Long id,
  @NotBlank String name,
  @NotBlank String code,
  @NotNull @JsonProperty("is_active") Boolean isActive
) {}
