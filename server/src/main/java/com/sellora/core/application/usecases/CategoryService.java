package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.presentation.dtos.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public List<CategoryDto> getCategoryTree() {
    // Дістаємо тільки кореневі категорії
    List<Category> rootCategories = categoryRepository.findAllByParentIsNull();

    // Мапимо їх у DTO (діти підтягнуться автоматично через рекурсію)
    return rootCategories.stream()
      .map(this::mapToDto)
      .collect(Collectors.toList());
  }

  private CategoryDto mapToDto(Category category) {
    return CategoryDto.builder()
      .id(category.getId())
      .name(category.getName())
      .parentId(category.getParent() != null ? category.getParent().getId() : null)
      .children(category.getChildren().stream()
        .map(this::mapToDto) // <- Рекурсивний виклик для кожної підкатегорії
        .collect(Collectors.toList()))
      .build();
  }
}
