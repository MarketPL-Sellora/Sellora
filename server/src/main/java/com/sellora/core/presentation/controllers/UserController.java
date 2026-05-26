package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.AuthService;// Або твій сервіс
import com.sellora.core.domain.entities.User;
import com.sellora.core.presentation.dtos.ChangePasswordRequestDto;
import com.sellora.core.presentation.dtos.UpdateProfileRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final AuthService userService;

  @Operation(summary = "Оновлення профілю (email, avatar)")
  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<User> updateProfile(@Valid @RequestBody UpdateProfileRequestDto request) {
    User updatedUser = userService.updateProfile(request);
    return ResponseEntity.ok(updatedUser);
  }

  @Operation(summary = "Зміна пароля")
  @PatchMapping("/me/password")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
    userService.changePassword(request);
    return ResponseEntity.ok().build();
  }
}
