package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.GroupBuySessionService;
import com.sellora.core.presentation.controllers.GroupBuySessionController;
import com.sellora.core.presentation.dtos.GroupBuyCheckoutRequestDto;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupBuySessionControllerTest {

  @Mock
  private GroupBuySessionService sessionService;

  @InjectMocks
  private GroupBuySessionController sessionController;

  // ─── Допоміжні фабрики ──────────────────────────────────────────────────────

  private GroupBuySessionResponseDto makeSessionResponse(String uuid) {
    return new GroupBuySessionResponseDto(
      uuid, 100L, "Product Title", "img.jpg", BigDecimal.valueOf(100),
      3, 1, "ACTIVE", LocalDateTime.now().plusHours(24),
      true, LocalDateTime.now(), List.of(),
      null // paymentUrl
    );
  }

  private GroupBuyCheckoutRequestDto makeCheckoutDto(Long productId) {
    GroupBuyCheckoutRequestDto dto = new GroupBuyCheckoutRequestDto();
    dto.setProductId(productId);
    dto.setBuyerName("Іван");
    dto.setBuyerSurname("Іваненко");
    dto.setBuyerPhone("+380501234567");
    dto.setBuyerEmail("ivan@example.com");
    dto.setDeliveryType("PICKUP");
    dto.setPaymentMethod("CASH_ON_DELIVERY");
    dto.setQuantity(1);
    return dto;
  }

  // ─── getSessionByUuid ────────────────────────────────────────────────────────

  @Test
  void getSessionByUuid_ReturnsOk() {
    // Arrange
    String uuid = "test-uuid-123";
    GroupBuySessionResponseDto responseDto = makeSessionResponse(uuid);
    when(sessionService.getSessionDetails(uuid)).thenReturn(responseDto);

    // Act
    ResponseEntity<GroupBuySessionResponseDto> response = sessionController.getSessionByUuid(uuid);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(uuid, response.getBody().uuid());
    verify(sessionService, times(1)).getSessionDetails(uuid);
  }

  // ─── joinSession ─────────────────────────────────────────────────────────────

  @Test
  void joinSession_ValidRequest_ReturnsOk() {
    // Arrange
    String uuid = "session-uuid";
    Long userId = 1L;
    GroupBuyCheckoutRequestDto dto = makeCheckoutDto(100L);
    GroupBuySessionResponseDto responseDto = makeSessionResponse(uuid);
    when(sessionService.joinSession(uuid, userId, dto)).thenReturn(responseDto);

    // Act
    ResponseEntity<GroupBuySessionResponseDto> response = sessionController.joinSession(uuid, userId, dto);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(uuid, response.getBody().uuid());
    verify(sessionService, times(1)).joinSession(uuid, userId, dto);
  }

  // ─── createSession ───────────────────────────────────────────────────────────

  @Test
  void createSession_ValidDto_ReturnsCreated() {
    // Arrange
    Long userId = 1L;
    GroupBuyCheckoutRequestDto dto = makeCheckoutDto(100L);
    GroupBuySessionResponseDto responseDto = new GroupBuySessionResponseDto(
      "new-uuid", 100L, "Product", null, BigDecimal.TEN,
      3, 1, "ACTIVE", LocalDateTime.now().plusHours(24),
      true, LocalDateTime.now(), List.of(),
      null // paymentUrl
    );
    when(sessionService.createSession(dto, userId)).thenReturn(responseDto);

    // Act
    ResponseEntity<GroupBuySessionResponseDto> response = sessionController.createSession(dto, userId);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("new-uuid", response.getBody().uuid());
    verify(sessionService, times(1)).createSession(dto, userId);
  }

  @Test
  void createSession_WithOnlineCard_ReturnsPaymentUrl() {
    // Arrange
    Long userId = 1L;
    GroupBuyCheckoutRequestDto dto = makeCheckoutDto(100L);
    dto.setPaymentMethod("ONLINE_CARD");

    GroupBuySessionResponseDto responseDto = new GroupBuySessionResponseDto(
      "new-uuid", 100L, "Product", null, BigDecimal.TEN,
      3, 1, "ACTIVE", LocalDateTime.now().plusHours(24),
      true, LocalDateTime.now(), List.of(),
      "https://liqpay.ua/checkout?data=abc&signature=xyz" // paymentUrl
    );
    when(sessionService.createSession(dto, userId)).thenReturn(responseDto);

    // Act
    ResponseEntity<GroupBuySessionResponseDto> response = sessionController.createSession(dto, userId);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody().paymentUrl());
    verify(sessionService).createSession(dto, userId);
  }

  // ─── getUserSessions ─────────────────────────────────────────────────────────

  @Test
  void getUserSessions_WithStatusFilter_ReturnsOk() {
    // Arrange
    Long userId = 1L;
    String status = "COMPLETED";
    when(sessionService.getUserSessions(userId, status)).thenReturn(List.of());

    // Act
    ResponseEntity<List<GroupBuySessionResponseDto>> response = sessionController.getUserSessions(userId, status);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(sessionService, times(1)).getUserSessions(userId, status);
  }

  @Test
  void getUserSessions_WithoutStatusFilter_ReturnsAllSessions() {
    // Arrange
    Long userId = 1L;
    List<GroupBuySessionResponseDto> sessions = List.of(
      makeSessionResponse("uuid-1"),
      makeSessionResponse("uuid-2")
    );
    when(sessionService.getUserSessions(userId, null)).thenReturn(sessions);

    // Act
    ResponseEntity<List<GroupBuySessionResponseDto>> response = sessionController.getUserSessions(userId, null);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    verify(sessionService).getUserSessions(userId, null);
  }
}
