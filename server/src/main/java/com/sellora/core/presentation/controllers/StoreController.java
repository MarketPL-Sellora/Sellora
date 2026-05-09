package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.StoreService;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreResponseDto; // <--- ДОДАНО ІМПОРТ
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

  @Operation(summary = "Створення нового магазину")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Магазин успішно створено"),
    @ApiResponse(responseCode = "400", description = "Помилка валідації даних"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований")
  })
  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> createStore(
    @RequestBody CreateStoreRequest request,
    @AuthenticationPrincipal Long userId) {

    storeService.createStore(request, userId);

    return ResponseEntity.ok(Map.of(
      "message", "Магазин '" + request.getName() + "' успішно створено!",
      "status", "Ви отримали статус MERCHANT (Продавець)"
    ));
  }

  // --- НОВИЙ ЕНДПОІНТ ---

  @Operation(summary = "Отримання інформації про магазин за ID продавця")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано деталі магазину"),
    @ApiResponse(responseCode = "404", description = "Магазин не знайдено")
  })
  // Тут ми НЕ ставимо @PreAuthorize, щоб покупці могли переглядати сторінку магазину без реєстрації
  @GetMapping("/user/{userId}")
  public ResponseEntity<StoreResponseDto> getStoreByUserId(@PathVariable Long userId) {
    StoreResponseDto store = storeService.getStoreByUserId(userId);
    return ResponseEntity.ok(store);
  }
}
