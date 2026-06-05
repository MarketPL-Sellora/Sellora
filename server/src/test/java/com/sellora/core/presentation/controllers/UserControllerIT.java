package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class UserControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

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
  // PUT /api/v1/users/me (Оновлення профілю)
  // ─────────────────────────────────────────────

  @Test
  void updateProfile_WithoutAuth_Returns401() throws Exception {
    String updateJson = "{\"email\":\"new.email@test.com\", \"avatarUrl\":\"new_avatar.png\"}";

    mockMvc.perform(put("/api/v1/users/me")
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateJson))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void updateProfile_WithAuth_Returns200AndUpdatesData() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    String updateJson = "{\"email\":\"updated.buyer@gmail.com\", \"avatarUrl\":\"http://example.com/avatar.png\"}";

    mockMvc.perform(put("/api/v1/users/me")
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateJson)
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.email").value("updated.buyer@gmail.com"))
      .andExpect(jsonPath("$.avatarUrl").value("http://example.com/avatar.png"));

    // Перевіряємо в базі даних (транзакція відкотить цю зміну після тесту)
    var user = userRepository.findByEmail("updated.buyer@gmail.com");
    assertEquals("http://example.com/avatar.png", user.get().getAvatarUrl());
  }

  // ─────────────────────────────────────────────
  // PATCH /api/v1/users/me/password (Зміна пароля)
  // ─────────────────────────────────────────────

  @Test
  void changePassword_WithoutAuth_Returns401() throws Exception {
    String pwdJson = "{\"oldPassword\":\"12345678\", \"newPassword\":\"NewPass123!\"}";

    mockMvc.perform(patch("/api/v1/users/me/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(pwdJson))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void changePassword_WithCorrectOldPassword_Returns200() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    String pwdJson = "{\"oldPassword\":\"12345678\", \"newPassword\":\"NewSecurePass123!\"}";

    mockMvc.perform(patch("/api/v1/users/me/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(pwdJson)
        .cookie(cookie))
      .andExpect(status().isOk());
  }

  @Test
  void changePassword_WithWrongOldPassword_ReturnsBadRequestOrForbidden() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    String pwdJson = "{\"oldPassword\":\"WRONG_PASSWORD\", \"newPassword\":\"NewSecurePass123!\"}";

    // Залежно від вашої реалізації, це може бути 400 (Bad Request) або 403 (Forbidden)
    mockMvc.perform(patch("/api/v1/users/me/password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(pwdJson)
        .cookie(cookie))
      .andExpect(status().is4xxClientError());
  }
}
