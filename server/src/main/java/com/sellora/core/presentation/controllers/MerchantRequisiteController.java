package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.MerchantRequisiteService;
import com.sellora.core.presentation.dtos.MerchantRequisiteDto;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant_requisites")
@RequiredArgsConstructor
public class MerchantRequisiteController {

  private final MerchantRequisiteService requisiteService;

  @Operation(summary = "Створити реквізити мерчанта")
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<MerchantRequisiteDto> create(
    @Valid @RequestBody MerchantRequisiteDto request,
    @AuthenticationPrincipal Long userId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(requisiteService.create(request, userId));
  }

  @Operation(summary = "Оновити реквізити мерчанта за ID")
  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<MerchantRequisiteDto> update(
    @PathVariable Long id,
    @Valid @RequestBody MerchantRequisiteDto request,
    @AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(requisiteService.update(id, request, userId));
  }

  @Operation(summary = "Видалити реквізити мерчанта за ID")
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> delete(
    @PathVariable Long id,
    @AuthenticationPrincipal Long userId) {
    requisiteService.delete(id, userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Отримати всі реквізити поточного мерчанта")
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<MerchantRequisiteDto>> getAllMyRequisites(
    @AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(requisiteService.getAllByOwner(userId));
  }

  @Operation(summary = "Отримати конкретні реквізити за ID")
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<MerchantRequisiteDto> getById(
    @PathVariable Long id,
    @AuthenticationPrincipal Long userId) {

    // Захист: шукаємо тільки серед реквізитів, які належать юзеру
    MerchantRequisiteDto requisite = requisiteService.getAllByOwner(userId).stream()
      .filter(r -> r.id().equals(id))
      .findFirst()
      .orElseThrow(() -> new ResourceNotFoundException("Реквізити не знайдено або у вас немає доступу"));

    return ResponseEntity.ok(requisite);
  }
}
