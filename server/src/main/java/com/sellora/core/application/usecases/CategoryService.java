package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CategoryDto;
import com.sellora.core.presentation.dtos.CategoryRequestDto;
import com.sellora.core.presentation.dtos.CategoryResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository; // НОВЕ: додано репозиторій товарів

  // --- СТАРІ МЕТОДИ (залишаємо для дерева категорій) ---
  public List<CategoryDto> getCategoryTree() {
    List<Category> rootCategories = categoryRepository.findAllByParentIsNull();
    return rootCategories.stream()
      .map(this::mapToTreeDto)
      .collect(Collectors.toList());
  }

  private CategoryDto mapToTreeDto(Category category) {
    return CategoryDto.builder()
      .id(category.getId())
      .name(category.getName())
      .parentId(category.getParent() != null ? category.getParent().getId() : null)
      .children(category.getChildren().stream()
        .map(this::mapToTreeDto)
        .collect(Collectors.toList()))
      .build();
  }

  // --- НОВІ МЕТОДИ ДЛЯ ADMIN CRUD ---

  @Transactional
  public CategoryResponseDto createCategory(CategoryRequestDto dto) {
    // 1. Перевірка унікальності імені
    if (categoryRepository.existsByName(dto.name())) {
      throw new BadRequestException("Категорія з такою назвою вже існує");
    }

    Category category = new Category();
    category.setName(dto.name());

    // 2. Якщо передано parentId, шукаємо і встановлюємо батька
    if (dto.parentId() != null) {
      Category parent = categoryRepository.findById(dto.parentId())
        .orElseThrow(() -> new BadRequestException("Батьківську категорію не знайдено"));
      category.setParent(parent);
    }

    category = categoryRepository.save(category);
    return mapToResponseDto(category);
  }

  public CategoryResponseDto getById(Long id) {
    return categoryRepository.findById(id)
      .map(this::mapToResponseDto)
      .orElseThrow(() -> new ResourceNotFoundException("Категорію не знайдено"));
  }

  // Пагінація для адмін-панелі або списку
  public Page<CategoryResponseDto> getAll(Pageable pageable) {
    return categoryRepository.findAll(pageable).map(this::mapToResponseDto);
  }

  @Transactional
  public void deleteCategory(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new ResourceNotFoundException("Категорію не знайдено");
    }

    // Перевірка 1: чи є дочірні категорії?
    if (categoryRepository.existsByParentId(id)) {
      throw new BadRequestException("Неможливо видалити категорію, яка має підкатегорії");
    }

    // Перевірка 2: чи прив'язані до цієї категорії товари?
    if (productRepository.existsByCategoryId(id)) {
      throw new BadRequestException("Неможливо видалити категорію, до якої прив'язані товари");
    }

    categoryRepository.deleteById(id);
  }

  // Мапер для нової плоскої відповіді
  private CategoryResponseDto mapToResponseDto(Category c) {
    Long parentId = c.getParent() != null ? c.getParent().getId() : null;
    return new CategoryResponseDto(c.getId(), c.getName(), parentId, c.getCreatedAt(), c.getUpdatedAt());
  }
}
