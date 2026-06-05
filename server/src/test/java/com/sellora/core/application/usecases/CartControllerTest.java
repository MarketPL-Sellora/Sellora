package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.CartService;
import com.sellora.core.presentation.controllers.CartController;
import com.sellora.core.presentation.dtos.AddToCartDto;
import com.sellora.core.presentation.dtos.AddToCartResponseDto;
import com.sellora.core.presentation.dtos.CartResponseDto;
import com.sellora.core.presentation.dtos.UpdateCartQuantityDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartControllerTest {

  @Mock
  private CartService cartService;

  @InjectMocks
  private CartController cartController;

  @Test
  void addToCart_ReturnsOkStatusAndResponse() {
    AddToCartDto request = new AddToCartDto(1L, 2);
    AddToCartResponseDto mockResponse = new AddToCartResponseDto(1L, 2, BigDecimal.valueOf(200), BigDecimal.valueOf(200));

    when(cartService.addToCart(request)).thenReturn(mockResponse);

    ResponseEntity<AddToCartResponseDto> response = cartController.addToCart(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(cartService, times(1)).addToCart(request);
  }

  @Test
  void updateQuantity_ReturnsOkStatus() {
    UpdateCartQuantityDto request = new UpdateCartQuantityDto(5);
    AddToCartResponseDto mockResponse = new AddToCartResponseDto(1L, 5, BigDecimal.valueOf(500), BigDecimal.valueOf(500));

    when(cartService.updateQuantity(1L, request)).thenReturn(mockResponse);

    ResponseEntity<AddToCartResponseDto> response = cartController.updateQuantity(1L, request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(cartService, times(1)).updateQuantity(1L, request);
  }

  @Test
  void getCart_ReturnsCartData() {
    CartResponseDto mockResponse = new CartResponseDto(List.of(), BigDecimal.ZERO);

    when(cartService.getCart()).thenReturn(mockResponse);

    ResponseEntity<CartResponseDto> response = cartController.getCart();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(cartService, times(1)).getCart();
  }

  @Test
  void removeItem_ReturnsNoContent() {
    doNothing().when(cartService).removeItem(1L);

    ResponseEntity<Void> response = cartController.removeItem(1L);

    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(cartService, times(1)).removeItem(1L);
  }

  @Test
  void clearCart_ReturnsNoContent() {
    doNothing().when(cartService).clearCart();

    ResponseEntity<Void> response = cartController.clearCart();

    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(cartService, times(1)).clearCart();
  }
}
