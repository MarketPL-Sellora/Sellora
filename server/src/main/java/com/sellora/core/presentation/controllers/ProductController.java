package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ProductService;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.presentation.dtos.CreateProductDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService productService;

  @PostMapping
  public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductDto request) {
    Product created = productService.createProduct(request);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  // ОНОВЛЕНИЙ МЕТОД
  @GetMapping
  public ResponseEntity<Page<Product>> getProducts(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Double minPrice,
    @RequestParam(required = false) Double maxPrice,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    Page<Product> products = productService.filterProducts(keyword, minPrice, maxPrice, categoryId, page, size, sortBy, sortDir);
    return ResponseEntity.ok(products);
  }
}
