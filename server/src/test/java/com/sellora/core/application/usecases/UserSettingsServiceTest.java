package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.UserSettings;
import com.sellora.core.infrastructure.persistence.UserSettingsRepository;
import com.sellora.core.presentation.dtos.UserSettingsDto;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserSettingsServiceTest {

  @Mock
  private UserSettingsRepository userSettingsRepository;

  @InjectMocks
  private UserSettingsService userSettingsService;

  private final Long userId = 1L;

  @BeforeEach
  void setUp() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);

    // Використання lenient() запобігає UnnecessaryStubbingException
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(userId);

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void getUserSettings_ExistingSettings_ReturnsDto() {
    UserSettings existingSettings = new UserSettings(userId, false);
    when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(existingSettings));

    UserSettingsDto result = userSettingsService.getUserSettings();

    assertFalse(result.notifyEmailOnOrderStatus());
    verify(userSettingsRepository, never()).save(any());
  }

  @Test
  void getUserSettings_NewSettings_CreatesAndReturnsDefaultDto() {
    when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());
    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(i -> i.getArgument(0));

    UserSettingsDto result = userSettingsService.getUserSettings();

    assertTrue(result.notifyEmailOnOrderStatus());
    verify(userSettingsRepository, times(1)).save(any(UserSettings.class));
  }

  @Test
  void updateUserSettings_ExistingSettings_UpdatesAndReturnsDto() {
    UserSettings existingSettings = new UserSettings(userId, true);
    UserSettingsDto updateDto = new UserSettingsDto(false);

    when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(existingSettings));
    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(i -> i.getArgument(0));

    UserSettingsDto result = userSettingsService.updateUserSettings(updateDto);

    assertFalse(result.notifyEmailOnOrderStatus());
    verify(userSettingsRepository, times(1)).save(existingSettings);
  }

  @Test
  void updateUserSettings_NewSettings_CreatesUpdatesAndReturnsDto() {
    UserSettingsDto updateDto = new UserSettingsDto(false);

    when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());
    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(i -> i.getArgument(0));

    UserSettingsDto result = userSettingsService.updateUserSettings(updateDto);

    assertFalse(result.notifyEmailOnOrderStatus());
    verify(userSettingsRepository, times(1)).save(any(UserSettings.class));
  }

  @Test
  void getCurrentUserId_AnonymousUser_ThrowsException() {
    // Цей тест перезаписує SecurityContext, тому базові моки мають бути lenient
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn("anonymousUser");
    SecurityContextHolder.setContext(securityContext);

    assertThrows(UnauthorizedException.class, () -> userSettingsService.getUserSettings());
  }
}
