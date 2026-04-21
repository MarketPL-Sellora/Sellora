package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.domain.entities.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  @PostMapping("/checkout")
  public ResponseEntity<Order> checkout() {
    Order order = orderService.checkout(1L); // Хардкодимо userId = 1L
    return new ResponseEntity<>(order, HttpStatus.CREATED);
  }
}
