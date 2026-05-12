package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.CategoryService;
import com.sellora.core.presentation.dtos.CategoryDto;
import com.sellora.core.presentation.dtos.CategoryRequestDto;
import com.sellora.core.presentation.dtos.CategoryResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  // --- СТАРИЙ ЕНДПОІНТ ---
  @Operation(summary = "Отримати дерево категорій (публічно)")
  @GetMapping("/tree")
  public List<CategoryDto> getCategoryTree() {
    return categoryService.getCategoryTree();
  }

  // --- НОВІ ЕНДПОІНТИ ---

  @Operation(summary = "Створити нову категорію (Тільки ADMIN)")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<CategoryResponseDto> createCategory(@RequestBody CategoryRequestDto request) {
    CategoryResponseDto created = categoryService.createCategory(request);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  @Operation(summary = "Отримати категорію за ID")
  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Long id) {
    return ResponseEntity.ok(categoryService.getById(id));
  }

  @Operation(summary = "Отримати список всіх категорій з пагінацією")
  @GetMapping
  public ResponseEntity<Page<CategoryResponseDto>> getAllCategories(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    // Створюємо об'єкт сортування
    org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase(org.springframework.data.domain.Sort.Direction.ASC.name())
      ? org.springframework.data.domain.Sort.by(sortBy).ascending()
      : org.springframework.data.domain.Sort.by(sortBy).descending();

    // Створюємо об'єкт пагінації
    Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

    return ResponseEntity.ok(categoryService.getAll(pageable));
  }

  @Operation(summary = "Видалити категорію (Тільки ADMIN)")
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
