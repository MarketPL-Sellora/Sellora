package com.sellora.core.presentation.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  @PostMapping
  public ResponseEntity<String> createOrder() {
    // Заглушка для створення замовлення
    return ResponseEntity.ok("Order creation endpoint is working");
  }
}
