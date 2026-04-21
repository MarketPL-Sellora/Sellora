package com.sellora.core.presentation.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  @PostMapping("/login")
  public ResponseEntity<String> login() {
    // Заглушка для майбутньої авторизації
    return ResponseEntity.ok("Auth login endpoint is working");
  }
}
