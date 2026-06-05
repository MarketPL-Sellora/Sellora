package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.PromoCode;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.PromoCodeRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.controllers.PromoCodeController;
import com.sellora.core.presentation.dtos.PromoCodeDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromoCodeControllerTest {

  @Mock
  private PromoCodeRepository repository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private PromoCodeController controller;

  private User adminUser;
  private User normalUser;
  private PromoCode promoCode;
  private PromoCodeDto promoCodeDto;

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

    promoCode = new PromoCode();
    promoCode.setId(10L);
    promoCode.setCode("SALE20");
    promoCode.setDiscountType("PERCENTAGE");
    promoCode.setValue(BigDecimal.valueOf(20));
    promoCode.setIsActive(true);

    promoCodeDto = new PromoCodeDto(
      10L, "SALE20", "PERCENTAGE", BigDecimal.valueOf(20),
      null, null, null, 0, true, null
    );
  }

  // --- ТЕСТИ СТВОРЕННЯ (CREATE) ---

  @Test
  void create_Success_ReturnsCreatedPromoCode() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.existsByCode("SALE20")).thenReturn(false);

    // Імітуємо збереження
    when(repository.save(any(PromoCode.class))).thenAnswer(i -> {
      PromoCode p = i.getArgument(0);
      p.setId(10L);
      return p;
    });

    ResponseEntity<PromoCodeDto> response = controller.create(promoCodeDto, adminId);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("SALE20", response.getBody().code());
    verify(repository, times(1)).save(any(PromoCode.class));
  }

  @Test
  void create_NotAdmin_ThrowsForbiddenException() {
    when(userRepository.findById(normalId)).thenReturn(Optional.of(normalUser));

    assertThrows(ForbiddenException.class, () -> controller.create(promoCodeDto, normalId));
    verify(repository, never()).save(any());
  }

  @Test
  void create_DuplicateCode_ThrowsConflictException() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.existsByCode("SALE20")).thenReturn(true);

    assertThrows(ConflictException.class, () -> controller.create(promoCodeDto, adminId));
    verify(repository, never()).save(any());
  }

  // --- ТЕСТИ ОНОВЛЕННЯ (UPDATE) ---

  @Test
  void update_Success_ReturnsUpdatedPromoCode() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.findById(10L)).thenReturn(Optional.of(promoCode));
    when(repository.existsByCodeAndIdNot("SALE20", 10L)).thenReturn(false);

    ResponseEntity<PromoCodeDto> response = controller.update(10L, promoCodeDto, adminId);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("SALE20", response.getBody().code());
    verify(repository, times(1)).save(promoCode);
  }

  @Test
  void update_DuplicateCode_ThrowsConflictException() {
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(repository.findById(10L)).thenReturn(Optional.of(promoCode));
    // Імітуємо, що такий код вже зайнятий іншим промокодом
    when(repository.existsByCodeAndIdNot("SALE20", 10L)).thenReturn(true);

    assertThrows(ConflictException.class, () -> controller.update(10L, promoCodeDto, adminId));
    verify(repository, never()).save(any());
  }

  // --- ТЕСТИ ОТРИМАННЯ (GET) ---

  @Test
  void getAll_ReturnsPromoCodeList() {
    when(repository.findAll()).thenReturn(List.of(promoCode));

    ResponseEntity<List<PromoCodeDto>> response = controller.getAll();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals("SALE20", response.getBody().get(0).code());
  }

  @Test
  void getById_Success_ReturnsPromoCode() {
    when(repository.findById(10L)).thenReturn(Optional.of(promoCode));

    ResponseEntity<PromoCodeDto> response = controller.getById(10L);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("SALE20", response.getBody().code());
  }

  @Test
  void getById_NotFound_ThrowsException() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> controller.getById(99L));
  }

  @Test
  void getByCode_Success_ReturnsPromoCode() {
    when(repository.findByCode("SALE20")).thenReturn(Optional.of(promoCode));

    ResponseEntity<PromoCodeDto> response = controller.getByCode("SALE20");

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("SALE20", response.getBody().code());
  }
}
