package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.presentation.dtos.CheckoutRequestDto;
import com.sellora.core.presentation.dtos.OrderResponseDto; // <--- ІМПОРТ
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  private Long getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal == null || principal.equals("anonymousUser")) {
      throw new UnauthorizedException("Потрібна авторизація");
    }
    return (Long) principal;
  }

  @Operation(summary = "Оформлення замовлення (Checkout)")
  @PostMapping("/checkout")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<OrderResponseDto> checkout(
    @Valid @RequestBody CheckoutRequestDto requestDto // <-- Додали тіло запиту
  ) {
    Long userId = getCurrentUserId();

    OrderResponseDto response = orderService.checkout(userId, requestDto); // <-- Передаємо DTO в сервіс

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
