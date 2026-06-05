package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.UserSettings;
import com.sellora.core.infrastructure.persistence.UserSettingsRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class UserSettingsControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserSettingsRepository userSettingsRepository;

  // ─────────────────────────────────────────────
  // ХЕЛПЕРИ
  // ─────────────────────────────────────────────

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
    var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginJson))
      .andExpect(status().isOk())
      .andReturn();

    jakarta.servlet.http.Cookie servletCookie = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", servletCookie.getValue());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/user-settings/me
  // ─────────────────────────────────────────────

  @Test
  void getUserSettings_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/user-settings/me"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserSettings_WithAuth_ReturnsSettings() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    // Робимо GET запит. Якщо налаштувань не було, сервіс має їх створити за замовчуванням
    mockMvc.perform(get("/api/v1/user-settings/me")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.notifyEmailOnOrderStatus").exists()); // Або інше поле, залежно від DTO
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/user-settings/me
  // ─────────────────────────────────────────────

  @Test
  void updateUserSettings_WithoutAuth_Returns401() throws Exception {
    String settingsJson = "{\"notifyEmailOnOrderStatus\": false}";

    mockMvc.perform(put("/api/v1/user-settings/me")
        .contentType(MediaType.APPLICATION_JSON)
        .content(settingsJson))
      .andExpect(status().isUnauthorized());
  }
}
