package com.sellora.core.presentation.controllers;

import com.sellora.core.application.usecases.ReviewService;
import com.sellora.core.presentation.dtos.ReviewRequestDto;
import com.sellora.core.presentation.dtos.ReviewResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "Створити відгук на товар (тільки для покупців, які отримали товар)")
  @PostMapping("/{product_id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReviewResponseDto> createReview(
    @PathVariable("product_id") Long productId,
    @Valid @RequestBody ReviewRequestDto request,
    @AuthenticationPrincipal Long userId) {

    ReviewResponseDto response = reviewService.createReview(productId, userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "Видалити відгук (тільки автор або Admin)")
  @DeleteMapping("/{product_id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteReview(
    @PathVariable("product_id") Long productId,
    @AuthenticationPrincipal Long userId) {

    reviewService.deleteReview(productId, userId);
    return ResponseEntity.noContent().build();
  }

  // Зверни увагу: для /check ми самостійно отримуємо юзера, тому що ендпоінт має бути доступний і для гостей (щоб повернути NOT_AUTHENTICATED)

  @Operation(summary = "Оновити відгук (тільки автор)")
  @PutMapping("/{product_id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ReviewResponseDto> updateReview(
    @PathVariable("product_id") Long productId,
    @Valid @RequestBody ReviewRequestDto request,
    @AuthenticationPrincipal Long userId) {

    ReviewResponseDto response = reviewService.updateReview(productId, userId, request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Перевірити, чи може юзер залишити відгук")
  @GetMapping("/{product_id}/check")
  public ResponseEntity<com.sellora.core.presentation.dtos.ReviewCheckResponseDto> checkEligibility(
    @PathVariable("product_id") Long productId) {

    // Отримуємо ID поточного юзера вручну, щоб не кидало 401 для гостей
    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    Long userId = null;
    if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
      userId = (Long) auth.getPrincipal();
    }

    return ResponseEntity.ok(reviewService.checkEligibility(productId, userId));
  }
}
