package com.sellora.core.application.usecases;

import com.sellora.core.application.usecases.AuthService;
import com.sellora.core.domain.entities.User;
import com.sellora.core.presentation.controllers.UserController;
import com.sellora.core.presentation.dtos.ChangePasswordRequestDto;
import com.sellora.core.presentation.dtos.UpdateProfileRequestDto;
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
public class UserControllerTest {

  @Mock
  private AuthService authService;

  @InjectMocks
  private UserController userController;

  @Test
  void updateProfile_ReturnsUpdatedUser() {
    // Використовуємо mock замість конструктора, щоб уникнути помилок з аргументами record
    UpdateProfileRequestDto request = mock(UpdateProfileRequestDto.class);

    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("newemail@sellora.com");
    mockUser.setAvatarUrl("new_avatar.png");

    when(authService.updateProfile(request)).thenReturn(mockUser);

    ResponseEntity<User> response = userController.updateProfile(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockUser, response.getBody());
    verify(authService, times(1)).updateProfile(request);
  }

  @Test
  void changePassword_ReturnsOkStatus() {
    // Передаємо 2 аргументи (oldPassword, newPassword) відповідно до сигнатури record
    ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldPass123", "newPass123");

    doNothing().when(authService).changePassword(request);

    ResponseEntity<Void> response = userController.changePassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(authService, times(1)).changePassword(request);
  }
}
