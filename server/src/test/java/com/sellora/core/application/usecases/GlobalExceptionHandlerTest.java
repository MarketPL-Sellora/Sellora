package com.sellora.core.application.usecases;

import com.sellora.core.presentation.dtos.ApiErrorResponse;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.GlobalExceptionHandler;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleResourceNotFound_Returns404() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Товар не знайдено");

    ResponseEntity<ApiErrorResponse> response = handler.handleResourceNotFoundException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().errorCode());
    assertEquals("Товар не знайдено", response.getBody().message());
  }

  @Test
  void handleConflict_Returns409() {
    ConflictException ex = new ConflictException("Конфлікт статусів");

    ResponseEntity<ApiErrorResponse> response = handler.handleConflictException(ex);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("CONFLICT", response.getBody().errorCode());
  }

  @Test
  void handleBadRequest_Returns400() {
    BadRequestException ex = new BadRequestException("Невірний формат");

    ResponseEntity<ApiErrorResponse> response = handler.handleBadRequestException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().errorCode());
  }
}
