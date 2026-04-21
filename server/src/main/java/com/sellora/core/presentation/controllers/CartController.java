package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.CartService;
import com.sellora.core.domain.entities.CartItem;
import com.sellora.core.presentation.dtos.AddToCartDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
  private final CartService cartService;

  @PostMapping
  public ResponseEntity<CartItem> addToCart(@Valid @RequestBody AddToCartDto request) {
    // Хардкодимо userId = 1L для MVP (доки немає повноцінної JWT авторизації)
    CartItem added = cartService.addToCart(request, 1L);
    return new ResponseEntity<>(added, HttpStatus.CREATED);
  }
}
