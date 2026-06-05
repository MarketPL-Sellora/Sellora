package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.MerchantRequisiteService;
import com.sellora.core.presentation.controllers.MerchantRequisiteController;
import com.sellora.core.presentation.dtos.MerchantRequisiteDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MerchantRequisiteControllerTest {

  @Mock
  private MerchantRequisiteService requisiteService;

  @InjectMocks
  private MerchantRequisiteController controller;

  private final Long userId = 1L;
  private MerchantRequisiteDto sampleDto;

  @BeforeEach
  void setUp() {
    sampleDto = new MerchantRequisiteDto(
      100L, userId, "12345678", "UA123456789012345678901234567", "Test Bank", true, null
    );
  }

  // --- CREATE TESTS ---

  @Test
  void create_Success() {
    // Arrange
    MerchantRequisiteDto requestDto = new MerchantRequisiteDto(
      null, null, "12345678", "UA123456789012345678901234567", "Test Bank", true, null
    );

    when(requisiteService.create(any(MerchantRequisiteDto.class), eq(userId))).thenReturn(sampleDto);

    // Act
    ResponseEntity<MerchantRequisiteDto> response = controller.create(requestDto, userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(sampleDto, response.getBody());
    verify(requisiteService, times(1)).create(requestDto, userId);
  }

  // --- UPDATE TESTS ---

  @Test
  void update_Success() {
    // Arrange
    Long requisiteId = 100L;
    MerchantRequisiteDto requestDto = new MerchantRequisiteDto(
      null, null, "87654321", "UA9999", "New Bank", false, null
    );
    MerchantRequisiteDto updatedDto = new MerchantRequisiteDto(
      requisiteId, userId, "87654321", "UA9999", "New Bank", false, null
    );

    when(requisiteService.update(eq(requisiteId), any(MerchantRequisiteDto.class), eq(userId)))
      .thenReturn(updatedDto);

    // Act
    ResponseEntity<MerchantRequisiteDto> response = controller.update(requisiteId, requestDto, userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updatedDto, response.getBody());
    verify(requisiteService, times(1)).update(requisiteId, requestDto, userId);
  }

  // --- DELETE TESTS ---

  @Test
  void delete_Success() {
    // Arrange
    Long requisiteId = 100L;
    doNothing().when(requisiteService).delete(requisiteId, userId);

    // Act
    ResponseEntity<Void> response = controller.delete(requisiteId, userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(requisiteService, times(1)).delete(requisiteId, userId);
  }

  // --- GET ALL TESTS ---

  @Test
  void getAllMyRequisites_Success() {
    // Arrange
    List<MerchantRequisiteDto> list = List.of(sampleDto);
    when(requisiteService.getAllByOwner(userId)).thenReturn(list);

    // Act
    ResponseEntity<List<MerchantRequisiteDto>> response = controller.getAllMyRequisites(userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
    assertEquals(sampleDto, response.getBody().get(0));
    verify(requisiteService, times(1)).getAllByOwner(userId);
  }

  // --- GET BY ID TESTS ---

  @Test
  void getById_Success_ReturnsDto() {
    // Arrange
    Long requestedId = 100L;
    MerchantRequisiteDto dto2 = new MerchantRequisiteDto(
      200L, userId, "999", "UA999", "Bank2", false, null
    );

    // Контролер використовує getAllByOwner для пошуку конкретного реквізиту
    when(requisiteService.getAllByOwner(userId)).thenReturn(List.of(sampleDto, dto2));

    // Act
    ResponseEntity<MerchantRequisiteDto> response = controller.getById(requestedId, userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(sampleDto, response.getBody()); // Має знайти саме sampleDto (id=100)
    verify(requisiteService, times(1)).getAllByOwner(userId);
  }

  @Test
  void getById_NotFoundOrNoAccess_ThrowsException() {
    // Arrange
    Long requestedId = 999L; // Такого ID немає в списку
    when(requisiteService.getAllByOwner(userId)).thenReturn(List.of(sampleDto));

    // Act & Assert
    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
      controller.getById(requestedId, userId);
    });

    assertEquals("Реквізити не знайдено або у вас немає доступу", exception.getMessage());
    verify(requisiteService, times(1)).getAllByOwner(userId);
  }
}
