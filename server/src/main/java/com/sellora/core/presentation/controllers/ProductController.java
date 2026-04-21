package com.sellora.core.presentation.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  @GetMapping
  public ResponseEntity<String> getAllProducts() {
    // Заглушка для отримання списку товарів
    return ResponseEntity.ok("Product list endpoint is working");
  }
}
