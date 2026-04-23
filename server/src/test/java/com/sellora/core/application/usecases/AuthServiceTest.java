package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
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

  // Створюємо "заглушки" для всіх залежностей AuthService
  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private AuthService authService;

  // REGISTER

  @Test
  void register_Success() {
    // arrange
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("securePassword");

    //сервіс шукає користувача, return empty результат
    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

    when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword123");

    //act
    authService.register(request);

    // assert
    //ArgumentCaptor, щоб "спіймати" об'єкт User, який передається в репозиторій для збереження
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertEquals("test@sellora.com", savedUser.getEmail());
    assertEquals("hashedPassword123", savedUser.getPasswordHash(), "Пароль має бути захешованим!");
    assertEquals("BUYER", savedUser.getRole(), "Роль за замовчуванням має бути BUYER");
  }

  @Test
  void register_UserAlreadyExists_ThrowsException() {

    RegisterRequest request = new RegisterRequest();
    request.setEmail("exist@sellora.com");
    request.setPassword("password");

    // імітація існування користувача в БД
    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.register(request);
    });

    assertEquals("Користувач з таким email вже існує", exception.getMessage());
    //перевірка що метод SAVE не викликався
    verify(userRepository, never()).save(any(User.class));
  }

  // LOGIN

  @Test
  void login_Success() {

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

    LoginRequest request = new LoginRequest();
    request.setEmail("wrong@sellora.com");
    request.setPassword("password");

    // користувача немає в базі
    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());


    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
  }

  @Test
  void login_WrongPassword_ThrowsException() {
    // Arrange
    LoginRequest request = new LoginRequest();
    request.setEmail("test@sellora.com");
    request.setPassword("wrongPassword");

    User mockUser = new User();
    mockUser.setEmail("test@sellora.com");
    mockUser.setPasswordHash("hashedPassword123");

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
    // Імітуємо, що паролі не співпадають
    when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
    // Переконуємося, що токен НЕ генерувався
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void register_NullPassword_ThrowsIllegalArgumentException() {
    //запит де пароль буде null
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@sellora.com");
    request.setPassword(null);

    //юзера ще не існує
    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

    //коли буде хешуватись null то повернути помилку
    when(passwordEncoder.encode(null)).thenThrow(new IllegalArgumentException("rawPassword cannot be null"));


    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      authService.register(request);
    });

    // додаткова перевірка що запис не додався в БД
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void login_NullEmail_ThrowsException() {
    //логін з порожнім email
    LoginRequest request = new LoginRequest();
    request.setEmail(null);
    request.setPassword("somePassword");

    when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

    //пошук за null нічого не видасть, тому сервер повинен видати повідомлення
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      authService.login(request);
    });

    assertEquals("Невірний email або пароль", exception.getMessage());
    // токен не повинен генеруватись
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void login_NullRequestObject_ThrowsNullPointerException() {
    // контроллер передає порожній об'єкт
    LoginRequest request = null;

    // перерірка що видасть NullPointerException
    assertThrows(NullPointerException.class, () -> {
      authService.login(request);
    });
  }

}
