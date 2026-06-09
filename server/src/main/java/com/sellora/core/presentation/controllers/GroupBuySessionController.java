package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.GroupBuySessionService;
import com.sellora.core.presentation.dtos.GroupBuyCheckoutRequestDto;
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
  @GetMapping("/{uuid}")
  public ResponseEntity<GroupBuySessionResponseDto> getSessionByUuid(@PathVariable String uuid) {
    return ResponseEntity.ok(sessionService.getSessionDetails(uuid));
  }

  @Operation(summary = "Приєднатися до сесії та оформити замовлення")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{uuid}/join")
  public ResponseEntity<GroupBuySessionResponseDto> joinSession(
    @PathVariable String uuid,
    @AuthenticationPrincipal Long userId,
    @Valid @RequestBody GroupBuyCheckoutRequestDto requestDto) {

    return ResponseEntity.ok(sessionService.joinSession(uuid, userId, requestDto));
  }

  @Operation(summary = "Створити сесію та оформити замовлення")
  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<GroupBuySessionResponseDto> createSession(
    @Valid @RequestBody GroupBuyCheckoutRequestDto requestDto,
    @AuthenticationPrincipal Long userId) {

    return new ResponseEntity<>(sessionService.createSession(requestDto, userId), HttpStatus.CREATED);
  }

  @Operation(summary = "Отримати всі сесії групових покупок поточного користувача (історія)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/user")
  public ResponseEntity<List<GroupBuySessionResponseDto>> getUserSessions(
    @AuthenticationPrincipal Long userId,
    @RequestParam(required = false) String status) {

    return ResponseEntity.ok(sessionService.getUserSessions(userId, status));
  }
}
