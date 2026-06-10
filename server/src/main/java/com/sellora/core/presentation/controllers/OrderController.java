package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.presentation.dtos.*;
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

import java.util.List;

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
  @PostMapping("/cart/checkout")
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

  // НОВИЙ ЕНДПОІНТ
  @Operation(summary = "Оновлення статусу замовлення та ТТН (тільки для продавця)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Замовлення успішно оновлено"),
    @ApiResponse(responseCode = "400", description = "Помилка валідації статусів або спроба змінити статус онлайн-оплати"),
    @ApiResponse(responseCode = "403", description = "Недостатньо прав (тільки власник магазину)"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{orderId}")
  public ResponseEntity<OrderResponseDto> updateOrder(
    @PathVariable Long orderId,
    @RequestBody UpdateOrderRequestDto request,
    @AuthenticationPrincipal Long requesterId) {

    OrderResponseDto response = orderService.updateOrder(orderId, requesterId, request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Отримати посилання для повторної оплати замовлення")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Посилання успішно згенеровано"),
    @ApiResponse(responseCode = "400", description = "Некоректний метод оплати або статус замовлення"),
    @ApiResponse(responseCode = "403", description = "Недостатньо прав (тільки покупець)"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{orderId}/pay")
  public ResponseEntity<PaymentUrlResponseDto> retryPayment(
    @PathVariable Long orderId,
    @AuthenticationPrincipal Long requesterId) {

    PaymentUrlResponseDto response = orderService.retryPayment(orderId, requesterId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Скасування замовлення покупцем")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Замовлення успішно скасовано"),
    @ApiResponse(responseCode = "400", description = "Неможливо скасувати (вже відправлено або скасовано)"),
    @ApiResponse(responseCode = "403", description = "Спроба скасувати чуже замовлення"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{orderId}/cancel")
  public ResponseEntity<OrderCancelResponseDto> cancelOrder(
    @PathVariable Long orderId,
    @AuthenticationPrincipal Long requesterId) {

    OrderCancelResponseDto response = orderService.cancelOrder(orderId, requesterId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Отримання історії транзакцій замовлення (тільки для продавця/адміна)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано список транзакцій"),
    @ApiResponse(responseCode = "403", description = "Недостатньо прав (не власник магазину і не Admin)"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{orderId}/transactions")
  public ResponseEntity<List<TransactionEventDto>> getOrderTransactions(
    @PathVariable Long orderId,
    @AuthenticationPrincipal Long requesterId) {

    List<TransactionEventDto> response = orderService.getOrderTransactions(orderId, requesterId);
    return ResponseEntity.ok(response);
  }
}
