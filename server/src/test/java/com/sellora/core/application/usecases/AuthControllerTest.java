package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.AuthService;
import com.sellora.core.presentation.controllers.AuthController;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import com.sellora.core.presentation.exceptions.UserAlreadyExistsException;
import jakarta.servlet.http.HttpServletResponse; // Використовуйте javax.servlet.http.* для старих версій Spring Boot
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

  @Mock
  private AuthService authService;

  // Додаємо мок для другого аргументу (припускаємо, що це HttpServletResponse)
  @Mock
  private HttpServletResponse response;

  @InjectMocks
  private AuthController authController;

  // --- REGISTER TESTS ---

  @Test
  void register_Success() {
    // Arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("securePassword");

    doNothing().when(authService).register(any(RegisterRequest.class));

    // Act
    // Якщо register також приймає 2 аргументи, додаємо response. Якщо 1 - залишаємо без змін.
    ResponseEntity<?> responseEntity = authController.register(request);

    // Assert
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    verify(authService, times(1)).register(request);
  }

  @Test
  void register_UserAlreadyExists_ThrowsException() {
    // Arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("exist@sellora.com");
    request.setPassword("password");

    doThrow(new UserAlreadyExistsException("Користувач існує"))
      .when(authService).register(any(RegisterRequest.class));

    // Act & Assert
    assertThrows(UserAlreadyExistsException.class, () -> {
      authController.register(request);
    });

    verify(authService, times(1)).register(request);
  }

  // --- LOGIN TESTS ---

  @Test
  void login_Success() {
    // Arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("securePassword");

    String mockToken = "mocked-jwt-token";
    when(authService.login(any(LoginRequest.class))).thenReturn(mockToken);

    // Act
    // Передаємо замоканий response як другий аргумент
    ResponseEntity<?> responseEntity = authController.login(request, response);

    // Assert
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    verify(authService, times(1)).login(request);
  }

  @Test
  void login_InvalidCredentials_ThrowsException() {
    // Arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("wrong@sellora.com");
    request.setPassword("wrongPass");

    when(authService.login(any(LoginRequest.class)))
      .thenThrow(new RuntimeException("Невірний email або пароль"));

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      // Передаємо замоканий response як другий аргумент
      authController.login(request, response);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
    verify(authService, times(1)).login(request);
  }
}
