package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ProductService;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
import com.sellora.core.presentation.dtos.UpdateProductDto;
import com.sellora.core.presentation.dtos.UpdateProductStatusDto;
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
  public ResponseEntity<Page<ProductResponseDto>> getProducts(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) BigDecimal minPrice,
    @RequestParam(required = false) BigDecimal maxPrice,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Long storeId,
    @RequestParam(required = false, defaultValue = "ALL") String groupMode,
    @RequestParam(required = false, defaultValue = "false") boolean onlyFavorites, // <--- НОВИЙ ПАРАМЕТР
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    Page<ProductResponseDto> products = productService.filterProducts(
      keyword, minPrice, maxPrice, categoryId,
      status, storeId, groupMode, onlyFavorites,
      page, size, sortBy, sortDir
    );

    return ResponseEntity.ok(products);
  }

  @Operation(summary = "Отримання списку товарів конкретного продавця (магазину)")
  @GetMapping("/merchant/{merchantId}")
  public ResponseEntity<Page<ProductResponseDto>> getProductsByMerchant(
    @PathVariable Long merchantId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    Page<ProductResponseDto> products = productService.getProductsByMerchant(merchantId, page, size, sortBy, sortDir);
    return ResponseEntity.ok(products);
  }

  @Operation(summary = "Отримання розгорнутої інформації про конкретний товар")
  @GetMapping("/{id}")
  public ResponseEntity<ProductResponseDto> getProductById(
    @PathVariable Long id,
    @RequestParam(defaultValue = "false") boolean full) {

    ProductResponseDto product = productService.getProductById(id, full);
    return ResponseEntity.ok(product);
  }

  @Operation(summary = "Оновлення існуючого товару")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Товар успішно оновлено"),
    @ApiResponse(responseCode = "403", description = "Спроба редагувати чужий товар"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}")
  public ResponseEntity<Product> updateProduct(
    @PathVariable Long id,
    @Valid @RequestBody UpdateProductDto request) {

    Product updatedProduct = productService.updateProduct(id, request);
    return ResponseEntity.ok(updatedProduct);
  }

  @Operation(summary = "Видалення товару")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Товар успішно видалено"),
    @ApiResponse(responseCode = "403", description = "Спроба видалити чужий товар"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено"),
    @ApiResponse(responseCode = "409", description = "Товар має активні групові сесії (Conflict)")
  })
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Зміна статусу товару (ACTIVE / ARCHIVED)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Статус успішно змінено"),
    @ApiResponse(responseCode = "400", description = "Помилка (наприклад, спроба активувати товар з 0 залишком)"),
    @ApiResponse(responseCode = "403", description = "Спроба редагувати чужий товар"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/status")
  public ResponseEntity<Product> updateProductStatus(
    @PathVariable Long id,
    @Valid @RequestBody UpdateProductStatusDto request) {

    Product updatedProduct = productService.updateProductStatus(id, request.status());
    return ResponseEntity.ok(updatedProduct);
  }
}
