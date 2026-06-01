package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.CartService;
import com.sellora.core.presentation.dtos.AddToCartDto;
import com.sellora.core.presentation.dtos.AddToCartResponseDto;
import com.sellora.core.presentation.dtos.CartResponseDto;
import com.sellora.core.presentation.dtos.UpdateCartQuantityDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // Можна винести на рівень класу, щоб захистити всі ендпоінти
public class CartController {

  private final CartService cartService;

  @Operation(summary = "Додати товар у кошик (UPSERT логіка)")
  @PostMapping
  public ResponseEntity<AddToCartResponseDto> addToCart(@Valid @RequestBody AddToCartDto request) {
    return ResponseEntity.ok(cartService.addToCart(request));
  }

  @Operation(summary = "Змінити кількість конкретного товару")
  @PatchMapping("/{productId}/quantity")
  public ResponseEntity<AddToCartResponseDto> updateQuantity(
    @PathVariable Long productId,
    @Valid @RequestBody UpdateCartQuantityDto request) {
    return ResponseEntity.ok(cartService.updateQuantity(productId, request));
  }

  @Operation(summary = "Отримати вміст кошика та суми")
  @GetMapping
  public ResponseEntity<CartResponseDto> getCart() {
    return ResponseEntity.ok(cartService.getCart());
  }

  @Operation(summary = "Видалити конкретний товар з кошика")
  @DeleteMapping("/{productId}")
  public ResponseEntity<Void> removeItem(@PathVariable Long productId) {
    cartService.removeItem(productId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Очистити весь кошик")
  @DeleteMapping
  public ResponseEntity<Void> clearCart() {
    cartService.clearCart();
    return ResponseEntity.noContent().build();
  }
}
