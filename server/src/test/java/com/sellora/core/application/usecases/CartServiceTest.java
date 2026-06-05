package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Cart;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.AddToCartDto;
import com.sellora.core.presentation.dtos.AddToCartResponseDto;
import com.sellora.core.presentation.dtos.CartResponseDto;
import com.sellora.core.presentation.dtos.UpdateCartQuantityDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

  @Mock
  private CartRepository cartRepository;

  @Mock
  private CartItemRepository cartItemRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private CartService cartService;

  private final Long userId = 1L;
  private Cart cart;
  private Product product;

  @BeforeEach
  void setUp() {
    // Налаштування SecurityContext
    SecurityContext securityContext = mock(SecurityContext.class);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    // Базові дані для тестів
    cart = new Cart();
    cart.setId(100L);
    cart.setUserId(userId);

    product = new Product();
    product.setId(10L);
    product.setTitle("Test Product");
    product.setStandardPrice(BigDecimal.valueOf(100));
    product.setStockQuantity(50);
    product.setStatus("ACTIVE");
  }

  @Test
  void addToCart_NewItem_Success() {
    AddToCartDto dto = new AddToCartDto(10L, 2);

    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.empty());

    // Мокаємо збереження
    when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
    when(cartItemRepository.findByCartId(100L)).thenReturn(List.of());

    AddToCartResponseDto response = cartService.addToCart(dto);

    assertEquals(10L, response.productId());
    assertEquals(2, response.quantity());
    assertEquals(BigDecimal.valueOf(200), response.subTotal());
    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  void addToCart_ExistingItem_IncreasesQuantity() {
    AddToCartDto dto = new AddToCartDto(10L, 3);

    CartItem existingItem = new CartItem();
    existingItem.setCart(cart);
    existingItem.setProduct(product);
    existingItem.setQuantity(2); // Вже є 2 шт. у кошику

    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.of(existingItem));

    when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

    AddToCartResponseDto response = cartService.addToCart(dto);

    assertEquals(5, response.quantity()); // 2 + 3 = 5
    assertEquals(BigDecimal.valueOf(500), response.subTotal());
  }

  @Test
  void addToCart_NotActiveProduct_ThrowsBadRequest() {
    product.setStatus("ARCHIVED");
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    AddToCartDto dto = new AddToCartDto(10L, 1);

    assertThrows(BadRequestException.class, () -> cartService.addToCart(dto));
  }

  @Test
  void addToCart_InsufficientStock_ThrowsBadRequest() {
    product.setStockQuantity(1); // На складі лише 1
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.empty());

    AddToCartDto dto = new AddToCartDto(10L, 2); // Хочемо додати 2

    assertThrows(BadRequestException.class, () -> cartService.addToCart(dto));
  }

  @Test
  void updateQuantity_Success() {
    // Я припускаю, що UpdateCartQuantityDto це запис (record) з полем newQuantity
    UpdateCartQuantityDto dto = new UpdateCartQuantityDto(5);

    CartItem existingItem = new CartItem();
    existingItem.setCart(cart);
    existingItem.setProduct(product);
    existingItem.setQuantity(2);

    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.of(existingItem));
    when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

    AddToCartResponseDto response = cartService.updateQuantity(10L, dto);

    assertEquals(5, response.quantity());
    assertEquals(BigDecimal.valueOf(500), response.subTotal());
  }

  @Test
  void removeItem_Success() {
    when(productRepository.existsById(10L)).thenReturn(true);
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

    cartService.removeItem(10L);

    verify(cartItemRepository).deleteByCartIdAndProductId(100L, 10L);
  }

  @Test
  void clearCart_Success() {
    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

    cartService.clearCart();

    verify(cartItemRepository).deleteAllByCartId(100L);
  }

  @Test
  void getCart_ReturnsCorrectTotals() {
    CartItem item1 = new CartItem();
    item1.setProduct(product);
    item1.setQuantity(2); // 2 * 100 = 200

    Product product2 = new Product();
    product2.setId(11L);
    product2.setStandardPrice(BigDecimal.valueOf(300));
    product2.setStockQuantity(10);

    CartItem item2 = new CartItem();
    item2.setProduct(product2);
    item2.setQuantity(1); // 1 * 300 = 300

    when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartId(100L)).thenReturn(List.of(item1, item2));

    CartResponseDto response = cartService.getCart();

    assertEquals(2, response.items().size());
    assertEquals(BigDecimal.valueOf(500), response.totalAmount()); // 200 + 300 = 500
  }
}
