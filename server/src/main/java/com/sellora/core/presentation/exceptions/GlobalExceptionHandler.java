package com.sellora.core.presentation.exceptions;

import com.sellora.core.presentation.dtos.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Обробка помилок валідації DTO (напр. @Min, @NotBlank)
   * Повертає 400 Bad Request
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
    // Збираємо всі помилки полів у зрозумілий рядок
    String details = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .collect(Collectors.joining(", "));

    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "VALIDATION_ERROR",
      details
    );

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "BAD_REQUEST",
      "Некоректний формат даних у запиті (очікувалося число, а прийшов текст або невалідний JSON)"
    );
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  /**
   * Загальний обробник для всіх інших непередбачуваних помилок
   * Повертає 500 Internal Server Error
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleAllExceptions(Exception ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "INTERNAL_ERROR",
      ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }


}
