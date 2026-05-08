package com.sellora.core.presentation.dtos;

import jakarta.validation.constraints.NotNull;

public record CreateGroupBuySessionDto(
  @NotNull(message = "ID товару є обов'язковим")
  Long productId
) {}
