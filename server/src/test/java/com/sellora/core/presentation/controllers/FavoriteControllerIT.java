package com.sellora.core.presentation.controllers;

import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.UserFavorite;
import com.sellora.core.infrastructure.persistence.UserFavoriteRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class FavoriteControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserFavoriteRepository userFavoriteRepository;

  // ─────────────────────────────────────────────
  // ХЕЛПЕРИ
  // ─────────────────────────────────────────────

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
    var result = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginJson))
      .andExpect(status().isOk())
      .andReturn();

    jakarta.servlet.http.Cookie servletCookie = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", servletCookie.getValue());
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/favorites/{productId}
  // ─────────────────────────────────────────────

  @Test
  void addFavorite_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/favorites/1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void addFavorite_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/favorites/999999")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void addFavorite_Success_Returns201() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Long userId = 7L;
    Long productId = 1L;

    // ГАРАНТУЄМО, що товару немає в улюблених перед тестом (незалежно від seed-даних)
    userFavoriteRepository.deleteByUserIdAndProductId(userId, productId);
    assertFalse(userFavoriteRepository.existsByUserIdAndProductId(userId, productId));

    mockMvc.perform(post("/api/v1/favorites/" + productId)
        .cookie(cookie))
      .andExpect(status().isCreated());

    // Перевіряємо, що товар з'явився в БД
    assertTrue(userFavoriteRepository.existsByUserIdAndProductId(userId, productId));
  }

  @Test
  void addFavorite_AlreadyExists_Returns201() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Long userId = 7L;
    Long productId = 2L;

    // Додаємо товар безпосередньо через репозиторій
    UserFavorite favorite = new UserFavorite();
    favorite.setUserId(userId);
    favorite.setProductId(productId);
    userFavoriteRepository.save(favorite);

    // Виконуємо запит на додавання того ж товару
    mockMvc.perform(post("/api/v1/favorites/" + productId)
        .cookie(cookie))
      .andExpect(status().isCreated()); // Ідемпотентність: не падає, повертає успіх
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/favorites/{productId}
  // ─────────────────────────────────────────────

  @Test
  void removeFavorite_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/favorites/1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void removeFavorite_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(delete("/api/v1/favorites/999999")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void removeFavorite_Success_Returns204() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Long userId = 7L;
    Long productId = 3L;

    // Спочатку додаємо товар
    UserFavorite favorite = new UserFavorite();
    favorite.setUserId(userId);
    favorite.setProductId(productId);
    userFavoriteRepository.save(favorite);
    assertTrue(userFavoriteRepository.existsByUserIdAndProductId(userId, productId));

    // Видаляємо через API
    mockMvc.perform(delete("/api/v1/favorites/" + productId)
        .cookie(cookie))
      .andExpect(status().isNoContent());

    // Перевіряємо, що товар зник з БД
    assertFalse(userFavoriteRepository.existsByUserIdAndProductId(userId, productId));
  }

  @Test
  void removeFavorite_NotInFavorites_Returns204() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Long userId = 7L;
    Long productId = 4L;

    // ГАРАНТУЄМО, що товару немає в улюблених
    userFavoriteRepository.deleteByUserIdAndProductId(userId, productId);
    assertFalse(userFavoriteRepository.existsByUserIdAndProductId(userId, productId));

    // Видаляємо те, чого немає
    mockMvc.perform(delete("/api/v1/favorites/" + productId)
        .cookie(cookie))
      .andExpect(status().isNoContent()); // Ідемпотентність: не падає
  }
}
