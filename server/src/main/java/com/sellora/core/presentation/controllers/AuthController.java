package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.AuthService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import com.sellora.core.presentation.dtos.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(Map.of("message", "Реєстрація успішна"));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
    String token = authService.login(request);

    // Налаштування безпечної куки
    ResponseCookie cookie = ResponseCookie.from("accessToken", token)
      .httpOnly(true)
      .secure(false) // Змінити на true, коли буде HTTPS на проді
      .path("/")
      .maxAge(2592000) // ЗМІНЕНО: 30 днів (у секундах)
      .sameSite("Lax")
      .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(Map.of("message", "Вхід успішний"));
  }

  // --- НОВИЙ ЕНДПОІНТ ---
  @Operation(summary = "Вихід з акаунта (Logout)")
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletResponse response) {
    // Створюємо "порожню" куку з таким самим ім'ям і шляхом, але з maxAge(0)
    ResponseCookie cookie = ResponseCookie.from("accessToken", "")
      .httpOnly(true)
      .secure(false) // Змінити на true на проді
      .path("/")
      .maxAge(0) // Вбиваємо куку миттєво
      .sameSite("Lax")
      .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(Map.of("message", "Успішний вихід з акаунта"));
  }


  @Operation(summary = "Отримання інформації про поточного авторизованого користувача")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Інформація про користувача отримана"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal Long userId) {
    UserResponseDto userResponse = authService.getCurrentUserInfo(userId);
    return ResponseEntity.ok(userResponse);
  }
}
