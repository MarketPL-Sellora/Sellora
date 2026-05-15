package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CategoryRequestDto;
import com.sellora.core.presentation.dtos.CategoryResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private CategoryService categoryService;

  @Test
  void createCategory_DuplicateName_ThrowsBadRequestException() {
    // Arrange
    CategoryRequestDto dto = new CategoryRequestDto("Electronics", null);
    when(categoryRepository.existsByName("Electronics")).thenReturn(true);

    // Act & Assert
    assertThrows(BadRequestException.class, () -> categoryService.createCategory(dto));
    verify(categoryRepository, never()).save(any());
  }

  @Test
  void createCategory_ValidRequest_SavesCategory() {
    // Arrange
    CategoryRequestDto dto = new CategoryRequestDto("Laptops", 1L);
    Category parent = new Category();
    parent.setId(1L);

    when(categoryRepository.existsByName("Laptops")).thenReturn(false);
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
      Category c = i.getArgument(0);
      c.setId(2L);
      return c;
    });

    // Act
    CategoryResponseDto result = categoryService.createCategory(dto);

    // Assert
    assertNotNull(result.id());
    assertEquals(1L, result.parentId());
    verify(categoryRepository).save(any(Category.class));
  }

  @Test
  void deleteCategory_HasSubcategories_ThrowsBadRequestException() {
    // Arrange
    Long catId = 1L;
    when(categoryRepository.existsById(catId)).thenReturn(true);
    when(categoryRepository.existsByParentId(catId)).thenReturn(true);

    // Act & Assert
    BadRequestException ex = assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(catId));
    assertEquals("Неможливо видалити категорію, яка має підкатегорії", ex.getMessage());
    verify(categoryRepository, never()).deleteById(any());
  }

  @Test
  void deleteCategory_HasLinkedProducts_ThrowsBadRequestException() {
    // Arrange
    Long catId = 1L;
    when(categoryRepository.existsById(catId)).thenReturn(true);
    when(categoryRepository.existsByParentId(catId)).thenReturn(false);
    // Імітуємо наявність товарів у цій категорії
    when(productRepository.existsByCategoryId(catId)).thenReturn(true);

    // Act & Assert
    BadRequestException ex = assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(catId));
    assertEquals("Неможливо видалити категорію, до якої прив'язані товари", ex.getMessage());
    verify(categoryRepository, never()).deleteById(any());
  }

  @Test
  void deleteCategory_EmptyCategory_DeletesSuccessfully() {
    // Arrange
    Long catId = 1L;
    when(categoryRepository.existsById(catId)).thenReturn(true);
    when(categoryRepository.existsByParentId(catId)).thenReturn(false);
    when(productRepository.existsByCategoryId(catId)).thenReturn(false);

    // Act
    categoryService.deleteCategory(catId);

    // Assert
    verify(categoryRepository).deleteById(catId);
  }

  @Test
  void getById_NotFound_ThrowsResourceNotFoundException() {
    // Arrange
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(99L));
  }



  @Test
  void createCategory_ParentNotFound_ThrowsBadRequestException() {
    CategoryRequestDto dto = new CategoryRequestDto("Laptops", 99L);
    when(categoryRepository.existsByName("Laptops")).thenReturn(false);
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> categoryService.createCategory(dto));
  }

  @Test
  void createCategory_ValidRequest_ReturnsResponseDto() {
    CategoryRequestDto dto = new CategoryRequestDto("Smartphones", null);
    Category savedCategory = new Category();
    savedCategory.setId(1L);
    savedCategory.setName("Smartphones");

    when(categoryRepository.existsByName("Smartphones")).thenReturn(false);
    when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

    CategoryResponseDto result = categoryService.createCategory(dto);

    assertNotNull(result);
    assertEquals(1L, result.id());
    assertEquals("Smartphones", result.name());
  }




  @Test
  void deleteCategory_ValidId_CallsDeleteById() {
    Long categoryId = 1L;
    when(categoryRepository.existsById(categoryId)).thenReturn(true);
    when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
    when(productRepository.existsByCategoryId(categoryId)).thenReturn(false);

    categoryService.deleteCategory(categoryId);

    verify(categoryRepository, times(1)).deleteById(categoryId);
  }
}
