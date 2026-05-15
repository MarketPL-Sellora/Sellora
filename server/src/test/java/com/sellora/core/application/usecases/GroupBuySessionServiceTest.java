package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.GroupBuySession;
import com.sellora.core.domain.entities.GroupMember;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateGroupBuySessionDto;
import com.sellora.core.presentation.dtos.GroupBuySessionResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupBuySessionServiceTest {

  @Mock
  private GroupBuySessionRepository sessionRepository;

  @Mock
  private GroupMemberRepository memberRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private GroupBuySessionService sessionService;

  // --- ТЕСТИ СТВОРЕННЯ СЕСІЇ ---

  @Test
  void createSession_ValidData_CreatesSessionAndReturnsDetails() {
    // Arrange
    Long initiatorId = 1L;
    Long productId = 100L;
    CreateGroupBuySessionDto dto = new CreateGroupBuySessionDto(productId);

    Product product = new Product();
    product.setId(productId);
    product.setTitle("Test Product");
    product.setGroupPrice(BigDecimal.valueOf(80));
    product.setGroupTargetSize(3);

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(memberRepository.isUserInActiveSessionForProduct(initiatorId, productId)).thenReturn(false);

    // Мокаємо збереження сесії
    when(sessionRepository.save(any(GroupBuySession.class))).thenAnswer(i -> {
      GroupBuySession s = i.getArgument(0);
      s.setId(10L);
      return s;
    });

    // Мокаємо виклики, які відбуваються всередині getSessionDetails (викликається в кінці createSession)
    when(sessionRepository.findByUuid(anyString())).thenAnswer(i -> {
      GroupBuySession s = new GroupBuySession();
      s.setId(10L);
      s.setUuid(i.getArgument(0));
      s.setProductId(productId);
      s.setStatus("ACTIVE");
      s.setLockedTargetSize(3);
      return Optional.of(s);
    });
    when(memberRepository.countBySessionId(10L)).thenReturn(1);
    when(memberRepository.findBySessionId(10L)).thenReturn(List.of(new GroupMember()));

    // Act
    GroupBuySessionResponseDto response = sessionService.createSession(dto, initiatorId);

    // Assert
    assertNotNull(response);
    assertEquals("ACTIVE", response.status());
    assertEquals(3, response.targetSize());

    // Перевіряємо, що ініціатор був доданий як учасник
    verify(memberRepository, times(1)).save(any(GroupMember.class));

    // Перевіряємо, що сесія збереглась із таймером на 24 години
    ArgumentCaptor<GroupBuySession> sessionCaptor = ArgumentCaptor.forClass(GroupBuySession.class);
    verify(sessionRepository, times(1)).save(sessionCaptor.capture());
    assertNotNull(sessionCaptor.getValue().getExpiresAt());
  }

  @Test
  void createSession_UserAlreadyInActiveSession_ThrowsBadRequestException() {
    // Arrange
    CreateGroupBuySessionDto dto = new CreateGroupBuySessionDto(100L);
    Product product = new Product();
    product.setId(100L);

    when(productRepository.findById(100L)).thenReturn(Optional.of(product));
    // Імітуємо, що користувач вже бере участь у зборі на цей товар
    when(memberRepository.isUserInActiveSessionForProduct(1L, 100L)).thenReturn(true);

    // Act & Assert
    BadRequestException exception = assertThrows(BadRequestException.class, () -> sessionService.createSession(dto, 1L));
    assertTrue(exception.getMessage().contains("Ви вже берете участь в активній сесії"));
    verify(sessionRepository, never()).save(any());
  }

  // --- ТЕСТИ ПРИЄДНАННЯ ДО СЕСІЇ ---

  @Test
  void joinSession_NotLastMember_AddsMemberAndKeepsActive() {
    // Arrange
    String uuid = UUID.randomUUID().toString();
    GroupBuySession session = new GroupBuySession();
    session.setId(10L);
    session.setUuid(uuid);
    session.setStatus("ACTIVE");
    session.setLockedTargetSize(3); // Потрібно 3 учасники

    when(sessionRepository.findByUuid(uuid)).thenReturn(Optional.of(session));
    when(memberRepository.existsBySessionIdAndUserId(10L, 2L)).thenReturn(false);
    // Імітуємо, що наразі в групі 1 учасник (після приєднання стане 2, тобто не 3)
    when(memberRepository.countBySessionId(10L)).thenReturn(1);

    // Act
    sessionService.joinSession(uuid, 2L);

    // Assert
    verify(memberRepository, times(1)).save(any(GroupMember.class));
    // Статус не повинен змінюватися, тому save для session не викликається
    verify(sessionRepository, never()).save(any(GroupBuySession.class));
  }

  @Test
  void joinSession_LastMember_AddsMemberAndCompletesSession() {
    // Arrange
    String uuid = UUID.randomUUID().toString();
    GroupBuySession session = new GroupBuySession();
    session.setId(10L);
    session.setUuid(uuid);
    session.setStatus("ACTIVE");
    session.setLockedTargetSize(3); // Потрібно 3 учасники

    when(sessionRepository.findByUuid(uuid)).thenReturn(Optional.of(session));
    when(memberRepository.existsBySessionIdAndUserId(10L, 3L)).thenReturn(false);
    // Імітуємо, що наразі в групі 2 учасники (поточний стане 3-м - останнім!)
    when(memberRepository.countBySessionId(10L)).thenReturn(2);

    // Act
    sessionService.joinSession(uuid, 3L);

    // Assert
    verify(memberRepository, times(1)).save(any(GroupMember.class));

    // Перевіряємо, що статус сесії змінився на COMPLETED і вона була збережена
    ArgumentCaptor<GroupBuySession> sessionCaptor = ArgumentCaptor.forClass(GroupBuySession.class);
    verify(sessionRepository, times(1)).save(sessionCaptor.capture());
    assertEquals("COMPLETED", sessionCaptor.getValue().getStatus());
  }

  @Test
  void joinSession_SessionAlreadyFull_ThrowsBadRequestException() {
    // Arrange
    String uuid = UUID.randomUUID().toString();
    GroupBuySession session = new GroupBuySession();
    session.setId(10L);
    session.setStatus("ACTIVE");
    session.setLockedTargetSize(3);

    when(sessionRepository.findByUuid(uuid)).thenReturn(Optional.of(session));
    when(memberRepository.existsBySessionIdAndUserId(10L, 2L)).thenReturn(false);
    // Імітуємо, що група вже заповнена (хоча статус чомусь ще ACTIVE)
    when(memberRepository.countBySessionId(10L)).thenReturn(3);

    // Act & Assert
    BadRequestException exception = assertThrows(BadRequestException.class, () -> sessionService.joinSession(uuid, 2L));
    assertEquals("У цій групі більше немає вільних місць", exception.getMessage());
    verify(memberRepository, never()).save(any());
  }

  @Test
  void joinSession_UserAlreadyMember_ThrowsBadRequestException() {
    // Arrange
    String uuid = UUID.randomUUID().toString();
    GroupBuySession session = new GroupBuySession();
    session.setId(10L);
    session.setStatus("ACTIVE");

    when(sessionRepository.findByUuid(uuid)).thenReturn(Optional.of(session));
    // Імітуємо, що юзер вже є в таблиці учасників цієї сесії
    when(memberRepository.existsBySessionIdAndUserId(10L, 1L)).thenReturn(true);

    // Act & Assert
    BadRequestException exception = assertThrows(BadRequestException.class, () -> sessionService.joinSession(uuid, 1L));
    assertEquals("Ви вже є учасником цієї групової покупки", exception.getMessage());
    verify(memberRepository, never()).save(any());
  }
}
