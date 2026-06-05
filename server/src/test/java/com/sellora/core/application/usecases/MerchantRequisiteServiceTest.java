package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.MerchantRequisite;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.MerchantRequisiteRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.MerchantRequisiteDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MerchantRequisiteServiceTest {

  @Mock
  private MerchantRequisiteRepository requisiteRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private MerchantRequisiteService requisiteService;

  private final Long userId = 1L;
  private User merchantUser;
  private MerchantRequisite requisite;

  @BeforeEach
  void setUp() {
    merchantUser = new User();
    merchantUser.setId(userId);
    merchantUser.setRole("MERCHANT");

    requisite = new MerchantRequisite();
    requisite.setId(100L);
    requisite.setOwnerId(userId);
    requisite.setEdrpou("12345678");
    requisite.setIban("UA123456789012345678901234567");
    requisite.setBankName("Test Bank");
    requisite.setIsPrimary(false);
  }

  @Test
  void create_NotMerchant_ThrowsForbiddenException() {
    merchantUser.setRole("BUYER");
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchantUser));

    MerchantRequisiteDto dto = new MerchantRequisiteDto(null, null, "123", "UA123", "Bank", true, null);

    assertThrows(ForbiddenException.class, () -> requisiteService.create(dto, userId));
    verify(requisiteRepository, never()).save(any());
  }

  @Test
  void create_WithIsPrimaryTrue_ResetsOldPrimary() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchantUser));

    MerchantRequisite oldPrimary = new MerchantRequisite();
    oldPrimary.setId(99L);
    oldPrimary.setIsPrimary(true);

    when(requisiteRepository.findByOwnerIdAndIsPrimaryTrue(userId)).thenReturn(Optional.of(oldPrimary));
    when(requisiteRepository.save(any(MerchantRequisite.class))).thenAnswer(i -> i.getArgument(0));

    MerchantRequisiteDto dto = new MerchantRequisiteDto(null, null, "123", "UA123", "Bank", true, null);
    requisiteService.create(dto, userId);

    assertFalse(oldPrimary.getIsPrimary());
    verify(requisiteRepository, times(2)).save(any(MerchantRequisite.class)); // 1 для oldPrimary, 1 для нового
  }

  @Test
  void update_NotOwner_ThrowsForbiddenException() {
    when(userRepository.findById(2L)).thenReturn(Optional.of(merchantUser)); // Авторизований юзер ID = 2
    when(requisiteRepository.findById(100L)).thenReturn(Optional.of(requisite)); // Власник реквізитів ID = 1

    MerchantRequisiteDto dto = new MerchantRequisiteDto(null, null, "123", "UA123", "Bank", false, null);

    assertThrows(ForbiddenException.class, () -> requisiteService.update(100L, dto, 2L));
  }

  @Test
  void delete_IsPrimary_ThrowsConflictException() {
    requisite.setIsPrimary(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchantUser));
    when(requisiteRepository.findById(100L)).thenReturn(Optional.of(requisite));

    assertThrows(ConflictException.class, () -> requisiteService.delete(100L, userId));
    verify(requisiteRepository, never()).delete(any());
  }

  @Test
  void delete_LastRequisite_ThrowsConflictException() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchantUser));
    when(requisiteRepository.findById(100L)).thenReturn(Optional.of(requisite));
    when(requisiteRepository.countByOwnerId(userId)).thenReturn(1L);

    assertThrows(ConflictException.class, () -> requisiteService.delete(100L, userId));
    verify(requisiteRepository, never()).delete(any());
  }

  @Test
  void delete_ValidRequest_Success() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(merchantUser));
    when(requisiteRepository.findById(100L)).thenReturn(Optional.of(requisite));
    when(requisiteRepository.countByOwnerId(userId)).thenReturn(2L);

    requisiteService.delete(100L, userId);

    verify(requisiteRepository, times(1)).delete(requisite);
  }

  @Test
  void getAllByOwner_ReturnsDtoList() {
    when(requisiteRepository.findByOwnerId(userId)).thenReturn(List.of(requisite));

    List<MerchantRequisiteDto> result = requisiteService.getAllByOwner(userId);

    assertEquals(1, result.size());
    assertEquals("12345678", result.get(0).edrpou());
  }
}
