package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ProductService;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService productService;
  private final ProductRepository productRepository;

  @PostMapping
  public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductDto request) {
    Product created = productService.createProduct(request);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  @GetMapping
  public List<Product> getAllProducts() {
    // Якщо ти використав nativeQuery в репозиторії, просто викликай цей метод
    return productRepository.findAllActiveProducts();
  }
}
