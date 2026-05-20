package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.domain.entities.Order;
import com.sellora.core.presentation.dtos.OrderDetailsResponseDto;
import com.sellora.core.presentation.dtos.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/checkout")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Order> checkout(@AuthenticationPrincipal Long userId) {
    Order order = orderService.checkout(userId);
    return new ResponseEntity<>(order, HttpStatus.CREATED);
  }

  @Operation(summary = "Отримання списку своїх замовлень (для покупця)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано список замовлень"),
    @ApiResponse(responseCode = "400", description = "Некоректні параметри пагінації або сортування"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований")
  })
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<OrderResponseDto>> getUserOrders(
    @AuthenticationPrincipal Long userId,
    @RequestParam(required = false) String shippingStatus,
    @RequestParam(required = false) String paymentStatus,
    @RequestParam(defaultValue = "0") @Min(value = 0, message = "Сторінка не може бути меншою за 0") int page,
    @RequestParam(defaultValue = "10") @Min(value = 1, message = "Розмір сторінки має бути мінімум 1") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "desc") String sortDir) {

    Page<OrderResponseDto> orders = orderService.getUserOrders(
      userId, shippingStatus, paymentStatus, page, size, sortBy, sortDir
    );

    return ResponseEntity.ok(orders);
  }

  @Operation(summary = "Отримання деталей конкретного замовлення")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано"),
    @ApiResponse(responseCode = "403", description = "Спроба доступу до чужого замовлення"),
    @ApiResponse(responseCode = "404", description = "Замовлення не знайдено")
  })
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<OrderDetailsResponseDto> getOrderById(
    @PathVariable Long id,
    @AuthenticationPrincipal Long userId) {

    OrderDetailsResponseDto order = orderService.getOrderById(id, userId);
    return ResponseEntity.ok(order);
  }
}
