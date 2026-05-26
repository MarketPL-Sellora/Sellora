package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.UserSettingsService;
import com.sellora.core.presentation.dtos.UserSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-settings")
@RequiredArgsConstructor
public class UserSettingsController {

  private final UserSettingsService userSettingsService;

  @Operation(summary = "Отримати налаштування поточного юзера")
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserSettingsDto> getMySettings() {
    return ResponseEntity.ok(userSettingsService.getUserSettings());
  }

  @Operation(summary = "Оновити налаштування поточного юзера")
  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserSettingsDto> updateMySettings(@Valid @RequestBody UserSettingsDto request) {
    return ResponseEntity.ok(userSettingsService.updateUserSettings(request));
  }
}
