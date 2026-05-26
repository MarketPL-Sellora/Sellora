package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtService;
import com.sellora.core.presentation.dtos.ChangePasswordRequestDto;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import com.sellora.core.presentation.dtos.UpdateProfileRequestDto;
import com.sellora.core.presentation.dtos.UserResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import com.sellora.core.presentation.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public void register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException("Користувач з таким email вже існує");
    }

    User user = new User();
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole("BUYER");

    userRepository.save(user);
  }

  public String login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
      .orElseThrow(() -> new UnauthorizedException("Невірний email або пароль"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Невірний email або пароль");
    }

    return jwtService.generateToken(user.getId());
  }

  public UserResponseDto getCurrentUserInfo(Long userId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    return new UserResponseDto(
      user.getId(),
      user.getEmail(),
      user.getRole(),
      user.getAvatarUrl(),
      user.getCreatedAt(),
      user.getUpdatedAt()
    );
  }

  // --- ХЕЛПЕР ДЛЯ ОТРИМАННЯ ID ПОТОЧНОГО КОРИСТУВАЧА ---
  private Long getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal == null || principal.equals("anonymousUser")) {
      throw new UnauthorizedException("Потрібна авторизація");
    }
    return (Long) principal;
  }

  // --- МЕТОДИ ДЛЯ ОНОВЛЕННЯ ПРОФІЛЮ ТА ЗМІНИ ПАРОЛЯ ---

  @Transactional
  public User updateProfile(UpdateProfileRequestDto dto) {
    Long userId = getCurrentUserId();
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    // Перевіряємо, чи не зайнятий новий email іншим юзером (ігноруємо, якщо email той самий)
    if (!user.getEmail().equalsIgnoreCase(dto.email()) && userRepository.existsByEmail(dto.email())) {
      throw new ConflictException("Цей email вже використовується іншим користувачем");
    }

    // Hard Update: записуємо нові значення (avatarUrl може бути null, і це ок)
    user.setEmail(dto.email());
    user.setAvatarUrl(dto.avatarUrl());

    return userRepository.save(user);
  }

  @Transactional
  public void changePassword(ChangePasswordRequestDto dto) {
    Long userId = getCurrentUserId();
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    // Звіряємо старий пароль з хешем у БД (віддаємо 400 Bad Request, як у ТЗ)
    if (!passwordEncoder.matches(dto.oldPassword(), user.getPasswordHash())) {
      throw new BadRequestException("Невірний старий пароль");
    }

    // Хешуємо новий пароль і зберігаємо
    user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
    userRepository.save(user);
  }
}
