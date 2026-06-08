package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.OrderService;
import com.sellora.core.application.usecases.StoreService;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreOrderPreviewDto;
import com.sellora.core.presentation.dtos.StoreResponseDto;
import com.sellora.core.presentation.dtos.UpdateStoreRequest;
import com.sellora.core.presentation.dtos.UpdateStoreStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
  private final OrderService orderService; // Інжект OrderService

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
    @Valid @RequestBody CreateStoreRequest request,
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
    return ResponseEntity.ok(storeService.getStoreByUserId(userId));
  }

  @Operation(summary = "Оновлення інформації про магазин")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Магазин успішно оновлено"),
    @ApiResponse(responseCode = "400", description = "Помилка валідації вхідних даних"),
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

    return ResponseEntity.ok(storeService.updateStore(storeId, request, userId));
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

    return ResponseEntity.ok(storeService.updateStoreStatus(storeId, request.status(), userId));
  }

  @Operation(summary = "Отримання списку всіх магазинів з пагінацією та пошуком")
  @GetMapping
  public ResponseEntity<Page<StoreResponseDto>> getAllStores(
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir) {

    return ResponseEntity.ok(storeService.getAllStores(keyword, page, size, sortBy, sortDir));
  }

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
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Отримання методів доставки конкретного магазину")
  @GetMapping("/{storeId}/shipping_methods")
  public ResponseEntity<Map<String, Object>> getStoreShippingMethods(@PathVariable Long storeId) {
    return ResponseEntity.ok(storeService.getStoreShippingMethods(storeId));
  }

  @Operation(summary = "Отримання списку замовлень магазину (для панелі продавця)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано"),
    @ApiResponse(responseCode = "403", description = "Не власник магазину і не Admin")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{storeId}/orders")
  public ResponseEntity<Page<StoreOrderPreviewDto>> getStoreOrders(
    @PathVariable Long storeId,
    @AuthenticationPrincipal Long requesterId,
    @RequestParam(required = false) String paymentStatus,
    @RequestParam(required = false) String shippingStatus,
    @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) { // <--- ДОДАНО @ParameterObject

    return ResponseEntity.ok(orderService.getStoreOrders(storeId, requesterId, paymentStatus, shippingStatus, pageable));
  }
}
