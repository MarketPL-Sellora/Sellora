package com.sellora.core.presentation.controllers;

import com.sellora.core.domain.entities.ShippingCarrier;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.ShippingCarrierRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.ShippingCarrierDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping_carriers")
@RequiredArgsConstructor
public class ShippingCarrierController {

  private final ShippingCarrierRepository repository;
  private final UserRepository userRepository;

  private void verifyAdmin(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (!"ADMIN".equalsIgnoreCase(user.getRole())) throw new ForbiddenException("Тільки ADMIN може керувати доставкою");
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ShippingCarrierDto> create(@Valid @RequestBody ShippingCarrierDto dto, @AuthenticationPrincipal Long userId) {
    verifyAdmin(userId);
    if (repository.existsByCode(dto.code())) throw new ConflictException("Служба з таким кодом вже існує");

    ShippingCarrier carrier = new ShippingCarrier();
    carrier.setName(dto.name());
    carrier.setCode(dto.code());
    carrier.setIsActive(dto.isActive());
    repository.save(carrier);

    return ResponseEntity.status(HttpStatus.CREATED).body(new ShippingCarrierDto(carrier.getId(), carrier.getName(), carrier.getCode(), carrier.getIsActive()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ShippingCarrierDto> update(@PathVariable Long id, @Valid @RequestBody ShippingCarrierDto dto, @AuthenticationPrincipal Long userId) {
    verifyAdmin(userId);
    ShippingCarrier carrier = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Службу не знайдено"));
    if (repository.existsByCodeAndIdNot(dto.code(), id)) throw new ConflictException("Служба з таким кодом вже існує");

    carrier.setName(dto.name());
    carrier.setCode(dto.code());
    carrier.setIsActive(dto.isActive());
    repository.save(carrier);

    return ResponseEntity.ok(new ShippingCarrierDto(carrier.getId(), carrier.getName(), carrier.getCode(), carrier.getIsActive()));
  }

  @GetMapping
  public ResponseEntity<List<ShippingCarrierDto>> getAll() {
    List<ShippingCarrierDto> list = repository.findAll().stream()
      .map(c -> new ShippingCarrierDto(c.getId(), c.getName(), c.getCode(), c.getIsActive())).toList();
    return ResponseEntity.ok(list);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ShippingCarrierDto> getById(@PathVariable Long id) {
    ShippingCarrier c = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Службу не знайдено"));
    return ResponseEntity.ok(new ShippingCarrierDto(c.getId(), c.getName(), c.getCode(), c.getIsActive()));
  }
}
