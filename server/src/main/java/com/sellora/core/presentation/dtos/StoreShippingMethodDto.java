package com.sellora.core.presentation.dtos;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record StoreShippingMethodDto(
  @NotNull @JsonProperty("carrier_id") Long carrierId,
  @NotNull @JsonProperty("is_enabled") Boolean isEnabled
) {}
