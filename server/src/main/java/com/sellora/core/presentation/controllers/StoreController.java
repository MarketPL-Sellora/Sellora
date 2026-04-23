package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.StoreService;
import com.sellora.core.domain.entities.User;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> createStore(
    @RequestBody CreateStoreRequest request,
    @AuthenticationPrincipal User currentUser) {

    storeService.createStore(request, currentUser);

    return ResponseEntity.ok(Map.of(
      "message", "Магазин '" + request.getName() + "' успішно створено!",
      "status", "Ви отримали статус MERCHANT (Продавець)"
    ));
  }
}
