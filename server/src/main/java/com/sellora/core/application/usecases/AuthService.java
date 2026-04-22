package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.infrastructure.security.JwtService;
import com.sellora.core.presentation.dtos.LoginRequest;
import com.sellora.core.presentation.dtos.RegisterRequest;
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
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new RuntimeException("Користувач з таким email вже існує");
    }

    User user = new User();
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Обов'язково хешуємо!
    user.setRole("BUYER"); // Значення за замовчуванням для БД, як домовилися

    userRepository.save(user);
  }

  public String login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
      .orElseThrow(() -> new RuntimeException("Невірний email або пароль"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new RuntimeException("Невірний email або пароль");
    }

    return jwtService.generateToken(user.getId());
  }
}
