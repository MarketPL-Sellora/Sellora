package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.UserFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

  private final UserFavoriteService favoriteService;

  @Operation(summary = "Додати товар в улюблені")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Товар успішно додано (або вже був в улюблених)"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено")
  })
  @PostMapping("/{productId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> addFavorite(@PathVariable Long productId) {
    favoriteService.addFavorite(productId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(summary = "Видалити товар з улюблених")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Товар успішно видалено (або його там і не було)"),
    @ApiResponse(responseCode = "401", description = "Користувач не авторизований"),
    @ApiResponse(responseCode = "404", description = "Товар не знайдено")
  })
  @DeleteMapping("/{productId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> removeFavorite(@PathVariable Long productId) {
    favoriteService.removeFavorite(productId);
    return ResponseEntity.noContent().build();
  }
}
