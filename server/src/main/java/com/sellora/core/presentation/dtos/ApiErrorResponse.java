package com.sellora.core.presentation.dtos;

public record ApiErrorResponse(
  String timestamp,
  String errorCode,
  String message
) {}
