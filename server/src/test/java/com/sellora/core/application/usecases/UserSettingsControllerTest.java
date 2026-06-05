package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.UserSettingsService;
import com.sellora.core.presentation.controllers.UserSettingsController;
import com.sellora.core.presentation.dtos.UserSettingsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserSettingsControllerTest {

  @Mock
  private UserSettingsService userSettingsService;

  @InjectMocks
  private UserSettingsController userSettingsController;

  @Test
  void getMySettings_ReturnsOkStatus() {
    UserSettingsDto mockResponse = new UserSettingsDto(true);
    when(userSettingsService.getUserSettings()).thenReturn(mockResponse);

    ResponseEntity<UserSettingsDto> response = userSettingsController.getMySettings();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(userSettingsService, times(1)).getUserSettings();
  }

  @Test
  void updateMySettings_ReturnsOkStatus() {
    UserSettingsDto requestDto = new UserSettingsDto(false);
    UserSettingsDto mockResponse = new UserSettingsDto(false);

    when(userSettingsService.updateUserSettings(requestDto)).thenReturn(mockResponse);

    ResponseEntity<UserSettingsDto> response = userSettingsController.updateMySettings(requestDto);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(userSettingsService, times(1)).updateUserSettings(requestDto);
  }
}
