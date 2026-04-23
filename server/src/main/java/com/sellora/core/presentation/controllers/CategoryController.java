package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.CategoryService;
import com.sellora.core.presentation.dtos.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  // Цей ендпоїнт публічний, бо меню бачать всі користувачі
  @GetMapping("/tree")
  public List<CategoryDto> getCategoryTree() {
    return categoryService.getCategoryTree();
  }
}
