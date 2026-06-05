package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.ShippingCarrier;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.ShippingCarrierRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.controllers.ShippingCarrierController;
import com.sellora.core.presentation.dtos.ShippingCarrierDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShippingCarrierControllerTest {

  @Mock
  private ShippingCarrierRepository repository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ShippingCarrierController controller;

  private User adminUser;
  private User normalUser;
  private ShippingCarrier carrier;
  private ShippingCarrierDto carrierDto;

  private final Long adminId = 1L;
  private final Long normalId = 2L;

  @BeforeEach
  void setUp() {
    adminUser = new User();
    adminUser.setId(adminId);
    adminUser.setRole("ADMIN");

    normalUser = new User();
    normalUser.setId(normalId);
    normalUser.setRole("BUYER");

    carrier = new ShippingCarrier();
    carrier.setId(10L);
    carrier.setName("Nova Poshta");
    carrier.setCode("NP");
    carrier.setIsActive(true);

    carrierDto = new ShippingCarrierDto(10L, "Nova Poshta", "NP", true);
  }

  // --- ТЕСТИ СТВОРЕННЯ (CREATE) ---

  @Test
  void create_Success_ReturnsCreatedCarrier() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.existsByCode("NP")).thenReturn(false);

    when(repository.save(any(ShippingCarrier.class))).thenAnswer(i -> {
      ShippingCarrier c = i.getArgument(0);
      c.setId(10L);
      return c;
    });

    ResponseEntity<ShippingCarrierDto> response = controller.create(carrierDto, adminId);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("NP", response.getBody().code());
    assertEquals("Nova Poshta", response.getBody().name());
    verify(repository, times(1)).save(any(ShippingCarrier.class));
  }

  @Test
  void create_NotAdmin_ThrowsForbiddenException() {
    when(userRepository.findById(normalId)).thenReturn(Optional.of(normalUser));

    assertThrows(ForbiddenException.class, () -> controller.create(carrierDto, normalId));
    verify(repository, never()).save(any());
  }

  @Test
  void create_DuplicateCode_ThrowsConflictException() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.existsByCode("NP")).thenReturn(true);

    assertThrows(ConflictException.class, () -> controller.create(carrierDto, adminId));
    verify(repository, never()).save(any());
  }

  // --- ТЕСТИ ОНОВЛЕННЯ (UPDATE) ---

  @Test
  void update_Success_ReturnsUpdatedCarrier() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.findById(10L)).thenReturn(Optional.of(carrier));
    when(repository.existsByCodeAndIdNot("NP", 10L)).thenReturn(false);

    ResponseEntity<ShippingCarrierDto> response = controller.update(10L, carrierDto, adminId);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("NP", response.getBody().code());
    verify(repository, times(1)).save(carrier);
  }

  @Test
  void update_DuplicateCode_ThrowsConflictException() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.findById(10L)).thenReturn(Optional.of(carrier));

    // Імітуємо, що код вже використовується іншою службою доставки
    when(repository.existsByCodeAndIdNot("NP", 10L)).thenReturn(true);

    assertThrows(ConflictException.class, () -> controller.update(10L, carrierDto, adminId));
    verify(repository, never()).save(any());
  }

  @Test
  void update_NotFound_ThrowsException() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> controller.update(99L, carrierDto, adminId));
  }

  // --- ТЕСТИ ОТРИМАННЯ (GET) ---

  @Test
  void getAll_ReturnsCarrierList() {
    when(repository.findAll()).thenReturn(List.of(carrier));

    ResponseEntity<List<ShippingCarrierDto>> response = controller.getAll();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("NP", response.getBody().get(0).code());
  }

  @Test
  void getById_Success_ReturnsCarrier() {
    when(repository.findById(10L)).thenReturn(Optional.of(carrier));

    ResponseEntity<ShippingCarrierDto> response = controller.getById(10L);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("NP", response.getBody().code());
  }

  @Test
  void getById_NotFound_ThrowsException() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> controller.getById(99L));
  }
}
