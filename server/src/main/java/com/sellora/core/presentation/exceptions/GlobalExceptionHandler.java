package com.sellora.core.presentation.exceptions;

import com.sellora.core.presentation.dtos.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Обробка помилок валідації DTO (напр. @Min, @NotBlank)
   * Повертає 400 Bad Request
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
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

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "EMAIL_ALREADY_EXISTS",
      ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "NOT_FOUND",
      ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "UNAUTHORIZED",
      ex.getMessage()
    );
    // Повертаємо статус 401 Unauthorized
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequestException(BadRequestException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "BAD_REQUEST",
      ex.getMessage()
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

  @ExceptionHandler(EmptyCartException.class)
  public ResponseEntity<Map<String, Object>> handleEmptyCartException(EmptyCartException ex) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("timestamp", LocalDateTime.now());
    errorResponse.put("errorCode", "EMPTY_CART");
    errorResponse.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiErrorResponse> handleForbiddenException(ForbiddenException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "FORBIDDEN",
      ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflictException(ConflictException ex) {
    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "CONFLICT",
      ex.getMessage()
    );
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  /**
   * Обробка помилок бази даних (Foreign Key, Check Constraints, Unique Constraints)
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    // Отримуємо найбільш детальне повідомлення від бази даних
    String rootMessage = ex.getMostSpecificCause().getMessage();
    String userMessage = "Помилка цілісності даних бази даних.";

    if (rootMessage != null) {
      String lowerCaseMessage = rootMessage.toLowerCase();

      if (lowerCaseMessage.contains("foreign key") || lowerCaseMessage.contains("fk_")) {
        userMessage = "Неможливо виконати дію (видалити або оновити), оскільки до цього запису вже прив'язана історія.";
      } else if (lowerCaseMessage.contains("check") && (lowerCaseMessage.contains("date") || lowerCaseMessage.contains("time"))) {
        userMessage = "Некоректний діапазон дат. Перевірте правильність введених часових рамок.";
      } else if (lowerCaseMessage.contains("unique") || lowerCaseMessage.contains("duplicate")) {
        userMessage = "Такий запис вже існує. Дані мають бути унікальними (наприклад, код промокоду чи назва).";
      } else {
        // Для інших CHECK обмежень (наприклад статусів)
        userMessage = "Недопустиме значення поля згідно з правилами бази даних.";
      }
    }

    ApiErrorResponse error = new ApiErrorResponse(
      LocalDateTime.now().toString(),
      "CONFLICT_DB_RELATION",
      userMessage
    );
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  /**
   * Загальний обробник для всіх інших непередбачуваних помилок
   * Повертає 500 Internal Server Error. Завжди має бути останнім.
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
