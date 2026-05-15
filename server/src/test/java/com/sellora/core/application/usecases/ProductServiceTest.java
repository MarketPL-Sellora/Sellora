package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.presentation.dtos.UpdateProductDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private GroupBuySessionRepository groupBuySessionRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private StoreRepository storeRepository;

  @InjectMocks
  private ProductService productService;

  @BeforeEach
  void setUpSecurityContext() {
    SecurityContext securityContext = mock(SecurityContext.class);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(1L, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  private void mockStoreExists() {
    Store mockStore = new Store();
    mockStore.setId(10L);
    when(storeRepository.findByOwnerId(1L)).thenReturn(Optional.of(mockStore));
  }

  @Test
  void createProduct_ValidData_SavesAndReturnsProduct() {
    // Arrange
    mockStoreExists();

    CreateProductDto dto = new CreateProductDto(
      "Samsung Galaxy S24", "Description", 1L,
      BigDecimal.valueOf(50000), BigDecimal.valueOf(45000),
      5, 10, null, null
    );

    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
      Product savedProduct = invocation.getArgument(0);
      savedProduct.setId(100L);
      return savedProduct;
    });

    // Act
    Product result = productService.createProduct(dto);

    // Assert
    assertNotNull(result.getId());
    assertEquals("Samsung Galaxy S24", result.getTitle());
    assertEquals(BigDecimal.valueOf(45000), result.getGroupPrice());
    assertEquals(10L, result.getMerchantId());

    verify(productRepository, times(1)).save(any(Product.class));
  }

  @Test
  void createProduct_StoreNotFound_ThrowsRuntimeException() {
    // Arrange
    when(storeRepository.findByOwnerId(1L)).thenReturn(Optional.empty());

    CreateProductDto dto = new CreateProductDto(
      "Item", "Desc", 1L, BigDecimal.valueOf(100), null, null, 10, null, null
    );

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      productService.createProduct(dto);
    });

    assertEquals("У вас немає активного магазину. Спочатку створіть магазин!", exception.getMessage());
    verify(productRepository, never()).save(any());
  }

  // --- ТЕСТИ DELETE PRODUCT ---

  @Test
  void deleteProduct_Success_DeletesProduct() {
    mockStoreExists(); // Імітує store.getId() == 10L
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(groupBuySessionRepository.existsByProductIdAndStatus(100L, "ACTIVE")).thenReturn(false);

    productService.deleteProduct(100L);

    verify(productRepository, times(1)).delete(product);
  }

  @Test
  void deleteProduct_WrongMerchant_ThrowsForbiddenException() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(99L); // Відрізняється від поточного магазину

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));

    assertThrows(ForbiddenException.class, () -> productService.deleteProduct(100L));
    // Явне вказування типу уникне помилки компіляції
    verify(productRepository, never()).delete(any(Product.class));
  }

  @Test
  void deleteProduct_ActiveGroupSessionsExist_ThrowsConflictException() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(groupBuySessionRepository.existsByProductIdAndStatus(100L, "ACTIVE")).thenReturn(true);

    assertThrows(ConflictException.class, () -> productService.deleteProduct(100L));
    // Явне вказування типу уникне помилки компіляції
    verify(productRepository, never()).delete(any(Product.class));
  }

  // --- ТЕСТИ UPDATE PRODUCT STATUS ---

  @Test
  void updateProductStatus_ToArchived_Success() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);
    product.setStatus("ACTIVE");

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

    Product result = productService.updateProductStatus(100L, "ARCHIVED");

    assertEquals("ARCHIVED", result.getStatus());
  }

  @Test
  void updateProductStatus_ToActiveWithZeroStock_ThrowsBadRequestException() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);
    product.setStatus("ARCHIVED");
    product.setStockQuantity(0);

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));

    assertThrows(BadRequestException.class, () -> productService.updateProductStatus(100L, "ACTIVE"));
    verify(productRepository, never()).save(any());
  }

  // --- ТЕСТИ UPDATE PRODUCT ---

  @Test
  void updateProduct_StockReachesZero_StatusChangesToOutOfStock() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);
    product.setCategoryId(1L);
    product.setStatus("ACTIVE");
    product.setStockQuantity(5);

    UpdateProductDto dto = new UpdateProductDto(
      "Title", "Desc", 1L, BigDecimal.valueOf(100), null, null,
      0, null, null // Встановлюємо 0 на складі
    );

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

    Product result = productService.updateProduct(100L, dto);

    assertEquals("OUT_OF_STOCK", result.getStatus());
    assertEquals(0, result.getStockQuantity());
  }

  @Test
  void updateProduct_StockReplenished_StatusChangesToActive() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);
    product.setCategoryId(1L);
    product.setStatus("OUT_OF_STOCK");
    product.setStockQuantity(0);

    UpdateProductDto dto = new UpdateProductDto(
      "Title", "Desc", 1L, BigDecimal.valueOf(100), null, null,
      10, null, null // Поповнюємо склад
    );

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

    Product result = productService.updateProduct(100L, dto);

    assertEquals("ACTIVE", result.getStatus());
    assertEquals(10, result.getStockQuantity());
  }

  @Test
  void updateProduct_ChangeToNonExistentCategory_ThrowsBadRequestException() {
    mockStoreExists();
    Product product = new Product();
    product.setId(100L);
    product.setMerchantId(10L);
    product.setCategoryId(1L);

    UpdateProductDto dto = new UpdateProductDto(
      "Title", "Desc", 99L, BigDecimal.valueOf(100), null, null,
      10, null, null
    );

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    when(categoryRepository.existsById(99L)).thenReturn(false);

    assertThrows(BadRequestException.class, () -> productService.updateProduct(100L, dto));
    verify(productRepository, never()).save(any());
  }
}
