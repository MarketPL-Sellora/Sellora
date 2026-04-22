package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.AuthService;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final StoreRepository storeRepository;

  @PatchMapping("/stores/{id}/status")
  public ResponseEntity<?> changeStoreStatus(@PathVariable Long id, @RequestParam String status) {
    return storeRepository.findById(id)
      .map(store -> {
        // Валідація: статус має бути PENDING, ACTIVE, BLOCKED або CLOSED
        store.setStatus(status.toUpperCase());
        storeRepository.save(store);
        return ResponseEntity.ok("Статус магазину змінено на " + status);
      })
      .orElse(ResponseEntity.notFound().build());
  }
}
