package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ProductService;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @Operation(summary = "Створення нового продукту")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Продукт успішно створено"),
    @ApiResponse(responseCode = "400", description = "Помилка валідації даних"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductDto request) {
    Product created = productService.createProduct(request);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  @Operation(summary = "Отримання списку продуктів з фільтрацією та пагінацією")
  @GetMapping
  public ResponseEntity<Page<Product>> getProducts(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) BigDecimal minPrice, // Змінено на BigDecimal
    @RequestParam(required = false) BigDecimal maxPrice, // Змінено на BigDecimal
    @RequestParam(required = false) Long categoryId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    Page<Product> products = productService.filterProducts(keyword, minPrice, maxPrice, categoryId, page, size, sortBy, sortDir);
    return ResponseEntity.ok(products);
  }

  // --- НОВИЙ ЕНДПОІНТ 1 ---
  @Operation(summary = "Отримання списку товарів конкретного продавця (магазину)")
  @GetMapping("/merchant/{merchantId}")
  public ResponseEntity<Page<Product>> getProductsByMerchant(
    @PathVariable Long merchantId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    Page<Product> products = productService.getProductsByMerchant(merchantId, page, size, sortBy, sortDir);
    return ResponseEntity.ok(products);
  }

  // --- НОВИЙ ЕНДПОІНТ 2 ---
  @Operation(summary = "Отримання розгорнутої інформації про конкретний товар")
  @GetMapping("/{id}")
  public ResponseEntity<ProductResponseDto> getProductById(
    @PathVariable Long id,
    @RequestParam(defaultValue = "false") boolean full) {

    ProductResponseDto product = productService.getProductById(id, full);
    return ResponseEntity.ok(product);
  }
}
