package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.GroupBuySessionService;
import com.sellora.core.presentation.dtos.CreateGroupBuySessionDto;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-buy/sessions")
@RequiredArgsConstructor
public class GroupBuySessionController {

  private final GroupBuySessionService sessionService;

  @Operation(summary = "Отримання деталей сесії за UUID для запрошення")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано деталі"),
    @ApiResponse(responseCode = "404", description = "Сесію не знайдено")
  })
  @GetMapping("/{uuid}")
  public ResponseEntity<GroupBuySessionResponseDto> getSessionByUuid(@PathVariable String uuid) {
    GroupBuySessionResponseDto sessionDto = sessionService.getSessionDetails(uuid);
    return ResponseEntity.ok(sessionDto);
  }

  @Operation(summary = "Приєднатися до сесії групової покупки")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно приєдналися"),
    @ApiResponse(responseCode = "401", description = "Необхідна авторизація"),
    @ApiResponse(responseCode = "404", description = "Сесію не знайдено"),
    @ApiResponse(responseCode = "400", description = "Немає місць або сесія неактивна")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{uuid}/join")
  public ResponseEntity<?> joinSession(
    @PathVariable String uuid,
    @AuthenticationPrincipal Long userId) {

    sessionService.joinSession(uuid, userId);
    return ResponseEntity.ok(java.util.Map.of("message", "Ви успішно приєдналися до групової покупки!"));
  }

  @Operation(summary = "Створити нову сесію групової покупки")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Сесію успішно створено"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<GroupBuySessionResponseDto> createSession(
    @Valid @RequestBody CreateGroupBuySessionDto dto,
    @AuthenticationPrincipal Long userId) {

    GroupBuySessionResponseDto createdSession = sessionService.createSession(dto, userId);
    return new ResponseEntity<>(createdSession, HttpStatus.CREATED);
  }

  // НОВЕ: Ендпоінт для історії сесій користувача
  @Operation(summary = "Отримати всі сесії групових покупок поточного користувача (історія)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Успішно отримано список сесій"),
    @ApiResponse(responseCode = "401", description = "Необхідна авторизація")
  })
  @PreAuthorize("isAuthenticated()") // Тільки для залогінених користувачів
  @GetMapping("/user")
  public ResponseEntity<List<GroupBuySessionResponseDto>> getUserSessions(
    @AuthenticationPrincipal Long userId,
    @RequestParam(required = false) String status) { // Опціональний фільтр статусу (ACTIVE, COMPLETED, CANCELLED)

    List<GroupBuySessionResponseDto> sessions = sessionService.getUserSessions(userId, status);
    return ResponseEntity.ok(sessions);
  }
}
