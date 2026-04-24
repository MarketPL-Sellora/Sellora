package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.StoreService;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
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
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований") // Тепер Swagger це побачить!
  })
  @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createStore(
            @RequestBody CreateStoreRequest request,
            @AuthenticationPrincipal Long userId) { // Змінено на Long

        storeService.createStore(request, userId); // Передаємо Long далі в сервіс

        return ResponseEntity.ok(Map.of(
                "message", "Магазин '" + request.getName() + "' успішно створено!",
                "status", "Ви отримали статус MERCHANT (Продавець)"
        ));
    }
}
