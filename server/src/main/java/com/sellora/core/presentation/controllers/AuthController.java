package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.AuthService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
      .maxAge(86400) // 24 години
      .sameSite("Lax")
      .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(Map.of("message", "Вхід успішний"));
  }
}
