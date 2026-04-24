package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import com.sellora.core.presentation.exceptions.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private AuthService authService;

  // --- REGISTER TESTS ---

  @Test
  void register_Success() {
    // arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("securePassword");

    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword123");

    // act
    authService.register(request);

    // assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("test@sellora.com", savedUser.getEmail());
    assertEquals("hashedPassword123", savedUser.getPasswordHash());
    assertEquals("BUYER", savedUser.getRole());
  }

  @Test
  void register_UserAlreadyExists_ThrowsException() {
    // arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("exist@sellora.com");
    request.setPassword("password");

    when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

    // act & assert
    assertThrows(UserAlreadyExistsException.class, () -> {
      authService.register(request);
    });

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void register_NullPassword_ThrowsIllegalArgumentException() {
    // arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@sellora.com");
    request.setPassword(null);

    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(null)).thenThrow(new IllegalArgumentException("rawPassword cannot be null"));

    // act & assert
    assertThrows(IllegalArgumentException.class, () -> {
      authService.register(request);
    });

    verify(userRepository, never()).save(any(User.class));
  }

  // --- LOGIN TESTS ---

  @Test
  void login_Success() {
    // arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("securePassword");

    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("test@sellora.com");
    mockUser.setPasswordHash("hashedPassword123");

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(mockUser.getId())).thenReturn("mocked-jwt-token");

    // act
    String token = authService.login(request);

    // assert
    assertEquals("mocked-jwt-token", token);
  }

  @Test
  void login_UserNotFound_ThrowsException() {
    // arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("wrong@sellora.com");
    request.setPassword("password");

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

    // act & assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
  }

  @Test
  void login_WrongPassword_ThrowsException() {
    // arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("wrongPassword");

    User mockUser = new User();
    mockUser.setEmail("test@sellora.com");
    mockUser.setPasswordHash("hashedPassword123");

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

    // act & assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void login_NullEmail_ThrowsException() {
    // arrange
    LoginRequest request = new LoginRequest();
    request.setEmail(null);
    request.setPassword("somePassword");

    // act & assert
    assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void login_NullRequestObject_ThrowsNullPointerException() {
    // act & assert
    assertThrows(NullPointerException.class, () -> {
      authService.login(null);
    });
  }
}
