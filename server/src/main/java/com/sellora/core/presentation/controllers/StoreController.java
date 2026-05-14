package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.StoreService;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreResponseDto;
import com.sellora.core.presentation.dtos.UpdateStoreRequest; // <--- ДОДАНО ІМПОРТ
import com.sellora.core.presentation.dtos.UpdateStoreStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid; // <--- ДОДАНО ІМПОРТ
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований"),
    @ApiResponse(responseCode = "409", description = "У вас вже є створений магазин")
  })
  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> createStore(
    @Valid @RequestBody CreateStoreRequest request, // ЗНОВУ ПРИЙМАЄМО JSON
    @AuthenticationPrincipal Long userId) {

    storeService.createStore(request, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
      "message", "Магазин '" + request.getName() + "' успішно створено!"
    ));
  }

  @Operation(summary = "Отримання інформації про магазин за ID продавця")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано деталі магазину"),
    @ApiResponse(responseCode = "404", description = "Магазин не знайдено")
  })
  @GetMapping("/user/{userId}")
  public ResponseEntity<StoreResponseDto> getStoreByUserId(@PathVariable Long userId) {
    StoreResponseDto store = storeService.getStoreByUserId(userId);
    return ResponseEntity.ok(store);
  }

  // --- НОВИЙ ЕНДПОІНТ: Оновлення магазину ---
  @Operation(summary = "Оновлення інформації про магазин")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Магазин успішно оновлено"),
    @ApiResponse(responseCode = "400", description = "Помилка валідації вхідних даних (наприклад, телефон)"),
    @ApiResponse(responseCode = "403", description = "Спроба оновити чужий магазин"),
    @ApiResponse(responseCode = "404", description = "Магазин не знайдено"),
    @ApiResponse(responseCode = "409", description = "Магазин з такою назвою вже існує")
  })
  @PutMapping("/{storeId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<StoreResponseDto> updateStore(
    @PathVariable Long storeId,
    @Valid @RequestBody UpdateStoreRequest request,
    @AuthenticationPrincipal Long userId) {

    StoreResponseDto updatedStore = storeService.updateStore(storeId, request, userId);
    return ResponseEntity.ok(updatedStore);
  }

  @Operation(summary = "Зміна статусу магазину (PENDING, ACTIVE, BLOCKED, CLOSED)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Статус успішно змінено"),
    @ApiResponse(responseCode = "400", description = "Некоректний статус"),
    @ApiResponse(responseCode = "403", description = "Недостатньо прав для цієї операції"),
    @ApiResponse(responseCode = "404", description = "Магазин не знайдено")
  })
  @PatchMapping("/{storeId}/status")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<StoreResponseDto> updateStoreStatus(
    @PathVariable Long storeId,
    @Valid @RequestBody UpdateStoreStatusRequest request,
    @AuthenticationPrincipal Long userId) {

    StoreResponseDto response = storeService.updateStoreStatus(storeId, request.status(), userId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Отримання списку всіх магазинів з пагінацією та пошуком")
  @GetMapping
  public ResponseEntity<org.springframework.data.domain.Page<StoreResponseDto>> getAllStores(
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    return ResponseEntity.ok(storeService.getAllStores(keyword, page, size, sortBy, sortDir));
  }

  // --- НОВИЙ ЕНДПОІНТ: Видалення магазину ---
  @Operation(summary = "Видалення магазину (Тільки для власника або ADMIN, якщо немає товарів)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Магазин успішно видалено"),
    @ApiResponse(responseCode = "403", description = "Немає прав на видалення"),
    @ApiResponse(responseCode = "404", description = "Магазин не знайдено"),
    @ApiResponse(responseCode = "409", description = "У магазину є існуючі товари")
  })
  @DeleteMapping("/{storeId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteStore(
    @PathVariable Long storeId,
    @AuthenticationPrincipal Long requesterId) {

    storeService.deleteStore(storeId, requesterId);
    return ResponseEntity.noContent().build(); // Повертає статус 204
  }
}
