package com.sellora.core.presentation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @GetMapping("/health")
  public ResponseEntity<String> checkHealth() {
    try {
      jdbcTemplate.execute("SELECT 1"); // Перевірка підключення до БД
      return ResponseEntity.ok("OK");
    } catch (Exception e) {
      // Якщо БД не відповідає — 503
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database is unavailable");
    }
  }
}
