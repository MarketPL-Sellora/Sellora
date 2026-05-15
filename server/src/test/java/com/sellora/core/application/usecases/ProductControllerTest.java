package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.ProductService;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.presentation.controllers.ProductController;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
import com.sellora.core.presentation.dtos.UpdateProductDto;
import com.sellora.core.presentation.dtos.UpdateProductStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

  @Mock
  private ProductService productService;

  @InjectMocks
  private ProductController productController;

  @Test
  void createProduct_ValidDto_ReturnsCreated() {
    // Arrange
    CreateProductDto dto = new CreateProductDto(
      "Title", "Description", 1L, BigDecimal.valueOf(100),
      BigDecimal.valueOf(80), 3, 10, List.of(), Map.of()
    );
    Product product = new Product();
    product.setId(1L);
    when(productService.createProduct(any(CreateProductDto.class))).thenReturn(product);

    // Act
    ResponseEntity<Product> response = productController.createProduct(dto);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().getId());
    verify(productService, times(1)).createProduct(dto);
  }

  @Test
  void getProducts_ReturnsOkWithPage() {
    // Arrange
    Page<Product> page = new PageImpl<>(List.of(new Product()));
    when(productService.filterProducts(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
      .thenReturn(page);

    // Act
    ResponseEntity<Page<Product>> response = productController.getProducts(
      "key", BigDecimal.ZERO, BigDecimal.TEN, 1L, "ACTIVE", 1L, "ALL", 0, 10, "id", "asc"
    );

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getTotalElements());
  }

  @Test
  void getProductById_ReturnsOkWithDto() {
    // Arrange
    ProductResponseDto dto = new ProductResponseDto(
      1L, "Title", "Desc", BigDecimal.TEN, BigDecimal.ONE, 5, 10, 1L, "Category", 1L, "Store", Map.of(), List.of(), "ACTIVE"
    );
    when(productService.getProductById(eq(1L), anyBoolean())).thenReturn(dto);

    // Act
    ResponseEntity<ProductResponseDto> response = productController.getProductById(1L, true);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().id());
  }

  @Test
  void updateProduct_ReturnsOk() {
    // Arrange
    UpdateProductDto dto = new UpdateProductDto(
      "New Title", "New Desc", 1L, BigDecimal.valueOf(200), null, null, 20, List.of(), Map.of()
    );
    Product updatedProduct = new Product();
    updatedProduct.setId(1L);
    when(productService.updateProduct(eq(1L), any(UpdateProductDto.class))).thenReturn(updatedProduct);

    // Act
    ResponseEntity<Product> response = productController.updateProduct(1L, dto);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void deleteProduct_ReturnsNoContent() {
    // Arrange
    doNothing().when(productService).deleteProduct(1L);

    // Act
    ResponseEntity<Void> response = productController.deleteProduct(1L);

    // Assert
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService, times(1)).deleteProduct(1L);
  }

  @Test
  void updateProductStatus_ReturnsOk() {
    // Arrange
    UpdateProductStatusDto dto = new UpdateProductStatusDto("ARCHIVED");
    Product product = new Product();
    product.setStatus("ARCHIVED");
    when(productService.updateProductStatus(eq(1L), eq("ARCHIVED"))).thenReturn(product);

    // Act
    ResponseEntity<Product> response = productController.updateProductStatus(1L, dto);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("ARCHIVED", response.getBody().getStatus());
  }
}
