package com.sellora.core.application.usecases;

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
    CreateProductDto dto = new CreateProductDto(
      "Title", "Description", 1L, BigDecimal.valueOf(100),
      BigDecimal.valueOf(80), 3, 10, List.of(), Map.of()
    );
    Product product = new Product();
    product.setId(1L);
    when(productService.createProduct(any(CreateProductDto.class))).thenReturn(product);

    ResponseEntity<Product> response = productController.createProduct(dto);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().getId());
    verify(productService, times(1)).createProduct(dto);
  }

  @Test
  void getProducts_ReturnsOkWithPage() {
    Page<ProductResponseDto> page = new PageImpl<>(List.of(
      new ProductResponseDto(1L, "Title", "Desc", BigDecimal.TEN, BigDecimal.ONE,
        5, 10, 1L, null, 1L, null, Map.of(), List.of(), "ACTIVE", false)
    ));
    when(productService.filterProducts(
      any(), any(), any(), any(), any(), any(), any(),
      anyBoolean(), // Додано параметр onlyFavorites
      anyInt(), anyInt(), anyString(), anyString()))
      .thenReturn(page);

    ResponseEntity<Page<ProductResponseDto>> response = productController.getProducts(
      "key", BigDecimal.ZERO, BigDecimal.TEN, 1L, "ACTIVE", 1L, "ALL",
      false, // Додано аргумент onlyFavorites
      0, 10, "id", "asc"
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getTotalElements());
  }

  @Test
  void getProductById_ReturnsOkWithDto() {
    ProductResponseDto dto = new ProductResponseDto(
      1L, "Title", "Desc", BigDecimal.TEN, BigDecimal.ONE, 5, 10,
      1L, "Category", 1L, "Store", Map.of(), List.of(), "ACTIVE",
      false // Додано параметр isFavorite
    );
    when(productService.getProductById(eq(1L), anyBoolean())).thenReturn(dto);

    ResponseEntity<ProductResponseDto> response = productController.getProductById(1L, true);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().id());
  }

  @Test
  void updateProduct_ReturnsOk() {
    UpdateProductDto dto = new UpdateProductDto(
      "New Title", "New Desc", 1L, BigDecimal.valueOf(200), null, null, 20, List.of(), Map.of()
    );
    Product updatedProduct = new Product();
    updatedProduct.setId(1L);
    when(productService.updateProduct(eq(1L), any(UpdateProductDto.class))).thenReturn(updatedProduct);

    ResponseEntity<Product> response = productController.updateProduct(1L, dto);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void deleteProduct_ReturnsNoContent() {
    doNothing().when(productService).deleteProduct(1L);

    ResponseEntity<Void> response = productController.deleteProduct(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(productService, times(1)).deleteProduct(1L);
  }

  @Test
  void updateProductStatus_ReturnsOk() {
    UpdateProductStatusDto dto = new UpdateProductStatusDto("ARCHIVED");
    Product product = new Product();
    product.setStatus("ARCHIVED");
    when(productService.updateProductStatus(eq(1L), eq("ARCHIVED"))).thenReturn(product);

    ResponseEntity<Product> response = productController.updateProductStatus(1L, dto);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("ARCHIVED", response.getBody().getStatus());
  }
}
