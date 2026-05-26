package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
import com.sellora.core.presentation.dtos.UserResponseDto;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException; // <--- ДОДАНО ІМПОРТ
import com.sellora.core.presentation.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}
