package com.sellora.core.presentation.exceptions;

import com.sellora.core.presentation.dtos.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleAllExceptions(Exception ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "INTERNAL_ERROR",
      ex.getMessage()
    );
    // Сервер повертає стандартизований JSON
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
