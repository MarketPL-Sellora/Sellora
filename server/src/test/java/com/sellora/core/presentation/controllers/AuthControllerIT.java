package com.sellora.core.presentation.controllers;

import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional; // Імпорт

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class AuthControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Test
  void registerAndLogin_SuccessFlow() throws Exception {
    String registerJson = "{\"email\":\"test@example.com\",\"password\":\"Password123!\"}";

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(registerJson))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.message").value("Реєстрація успішна"));

    assertTrue(userRepository.findByEmail("test@example.com").isPresent());

    String loginJson = "{\"email\":\"test@example.com\",\"password\":\"Password123!\"}";

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginJson))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message").value("Вхід успішний"))
      .andExpect(cookie().exists("accessToken"))
      .andExpect(cookie().httpOnly("accessToken", true));
  }
}
