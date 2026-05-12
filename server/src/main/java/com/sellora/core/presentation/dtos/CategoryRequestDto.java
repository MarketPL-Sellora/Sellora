package com.sellora.core.presentation.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryRequestDto(
  String name,
  @Schema(description = "ID батьківської категорії (залиште null, якщо це головна категорія)", nullable = true, example = "null")
  Long parentId

) {}
