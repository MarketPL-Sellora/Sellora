package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.GroupBuySessionService;
import com.sellora.core.presentation.controllers.GroupBuySessionController;
import com.sellora.core.presentation.dtos.CreateGroupBuySessionDto;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupBuySessionControllerTest {

  @Mock
  private GroupBuySessionService sessionService;

  @InjectMocks
  private GroupBuySessionController sessionController;

  @Test
  void getSessionByUuid_ReturnsOk() {
    // Arrange
    String uuid = "test-uuid-123";
    GroupBuySessionResponseDto responseDto = new GroupBuySessionResponseDto(
      uuid, 1L, "Product Title", "img.jpg", BigDecimal.valueOf(100),
      3, 1, "ACTIVE", LocalDateTime.now().plusHours(24),
      true, LocalDateTime.now(), List.of()
    );
    when(sessionService.getSessionDetails(uuid)).thenReturn(responseDto);

    // Act
    ResponseEntity<GroupBuySessionResponseDto> response = sessionController.getSessionByUuid(uuid);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(uuid, response.getBody().uuid());
    verify(sessionService, times(1)).getSessionDetails(uuid);
  }

  @Test
  @SuppressWarnings("unchecked")
  void joinSession_ValidRequest_ReturnsOkWithMessage() {
    // Arrange
    String uuid = "session-uuid";
    Long userId = 1L;
    doNothing().when(sessionService).joinSession(uuid, userId);

    // Act
    ResponseEntity<?> response = sessionController.joinSession(uuid, userId);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, String> body = (Map<String, String>) response.getBody();
    assertNotNull(body);
    assertEquals("Ви успішно приєдналися до групової покупки!", body.get("message"));
    verify(sessionService, times(1)).joinSession(uuid, userId);
  }

  @Test
  void createSession_ValidDto_ReturnsCreated() {
    // Arrange
    Long userId = 1L;
    CreateGroupBuySessionDto dto = new CreateGroupBuySessionDto(100L);
    GroupBuySessionResponseDto responseDto = new GroupBuySessionResponseDto(
      "new-uuid", 100L, "Product", null, BigDecimal.TEN, 3, 1, "ACTIVE",
      LocalDateTime.now().plusHours(24), true, LocalDateTime.now(), List.of()
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
}
