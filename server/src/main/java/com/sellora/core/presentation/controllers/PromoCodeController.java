package com.sellora.core.presentation.controllers;

import com.sellora.core.domain.entities.PromoCode;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.PromoCodeRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.PromoCodeDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
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
@RequestMapping("/api/v1/promo_codes")
@RequiredArgsConstructor
public class PromoCodeController {

  private final PromoCodeRepository repository;
  private final UserRepository userRepository;

  private void verifyAdmin(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (!"ADMIN".equalsIgnoreCase(user.getRole())) throw new ForbiddenException("Тільки ADMIN може керувати промокодами");
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PromoCodeDto> create(@Valid @RequestBody PromoCodeDto dto, @AuthenticationPrincipal Long userId) {
    // Валідація дат
    if (dto.startDate() != null && dto.endDate() != null) {
      if (dto.endDate().isBefore(dto.startDate())) {
        throw new BadRequestException("Дата завершення дії промокоду не може бути раніше дати початку");
      }
    }
    verifyAdmin(userId);
    if (repository.existsByCode(dto.code())) throw new ConflictException("Промокод вже існує");

    PromoCode promo = new PromoCode();
    promo.setCode(dto.code());
    promo.setDiscountType(dto.discountType());
    promo.setValue(dto.value());
    promo.setStartDate(dto.startDate());
    promo.setEndDate(dto.endDate());
    promo.setUsageLimit(dto.usageLimit());
    promo.setIsActive(dto.isActive());
    repository.save(promo);

    return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(promo));
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PromoCodeDto> update(@PathVariable Long id, @Valid @RequestBody PromoCodeDto dto, @AuthenticationPrincipal Long userId) {
    // Валідація дат
    if (dto.startDate() != null && dto.endDate() != null) {
      if (dto.endDate().isBefore(dto.startDate())) {
        throw new BadRequestException("Дата завершення дії промокоду не може бути раніше дати початку");
      }
    }
    verifyAdmin(userId);
    PromoCode promo = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Промокод не знайдено"));
    if (repository.existsByCodeAndIdNot(dto.code(), id)) throw new ConflictException("Промокод вже існує");

    promo.setCode(dto.code());
    promo.setDiscountType(dto.discountType());
    promo.setValue(dto.value());
    promo.setStartDate(dto.startDate());
    promo.setEndDate(dto.endDate());
    promo.setUsageLimit(dto.usageLimit());
    promo.setIsActive(dto.isActive());
    repository.save(promo);

    return ResponseEntity.ok(mapToDto(promo));
  }

  @GetMapping
  public ResponseEntity<List<PromoCodeDto>> getAll() {
    return ResponseEntity.ok(repository.findAll().stream().map(this::mapToDto).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<PromoCodeDto> getById(@PathVariable Long id) {
    PromoCode p = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Промокод не знайдено"));
    return ResponseEntity.ok(mapToDto(p));
  }

  private PromoCodeDto mapToDto(PromoCode p) {
    return new PromoCodeDto(p.getId(), p.getCode(), p.getDiscountType(), p.getValue(),
      p.getStartDate(), p.getEndDate(), p.getUsageLimit(), p.getUsedCount(), p.getIsActive(), p.getCreatedAt());
  }

  @Operation(summary = "Отримати промокод за його текстовим кодом")
  @GetMapping("/by-code/{code}")
  public ResponseEntity<PromoCodeDto> getByCode(@PathVariable String code) {
    PromoCode p = repository.findByCode(code)
      .orElseThrow(() -> new ResourceNotFoundException("Промокод не знайдено"));
    return ResponseEntity.ok(mapToDto(p));
  }
}
