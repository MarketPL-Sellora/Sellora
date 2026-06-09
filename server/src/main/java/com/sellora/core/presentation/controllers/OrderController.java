package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.presentation.dtos.CheckoutRequestDto;
import com.sellora.core.presentation.dtos.OrderPreviewDto;
import com.sellora.core.presentation.dtos.OrderResponseDto; // <--- ІМПОРТ
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
 import org.springframework.data.domain.Page;
 import org.springframework.data.domain.Pageable;
 import org.springframework.data.domain.Sort;
 import org.springframework.data.web.PageableDefault;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  @PostMapping("/cart/checkout") // ВІДПОВІДАЄ ТЗ
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<OrderResponseDto> checkout(@Valid @RequestBody CheckoutRequestDto requestDto) {
    Long userId = getCurrentUserId();
    OrderResponseDto response = orderService.checkout(userId, requestDto);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @Operation(summary = "Отримання історії замовлень поточного користувача")
  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<Page<OrderPreviewDto>> getUserOrderHistory(
    @AuthenticationPrincipal Long userId,
    @org.springdoc.core.annotations.ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) {

    Page<OrderPreviewDto> response = orderService.getUserOrdersHistory(userId, pageable);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Отримання деталей конкретного замовлення")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано"),
    @ApiResponse(responseCode = "403", description = "Недостатньо прав (не покупець і не продавець)"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponseDto> getOrderDetails(
    @PathVariable Long orderId,
    @AuthenticationPrincipal Long requesterId) {

    OrderResponseDto response = orderService.getOrderDetails(orderId, requesterId);
    return ResponseEntity.ok(response);
  }


}
