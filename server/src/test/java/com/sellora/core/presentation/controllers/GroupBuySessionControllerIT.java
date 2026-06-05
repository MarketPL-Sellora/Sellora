package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.GroupMemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Інтеграційні тести для GroupBuySessionController.
 *
 * СТРАТЕГІЯ: Кожен тест сам створює потрібні сесії через API,
 * не покладаючись на seed-сесії (які можуть бути змінені іншими тестами).
 *
 * Seed products (стабільні):
 *   - Product id=1 → ACTIVE, groupTargetSize=3, groupPrice=44000
 *   - Product id=2 → ACTIVE, groupTargetSize=3, groupPrice=47000
 *   - Product id=3 → ACTIVE, groupTargetSize=5, groupPrice=4800
 *
 * Users:
 *   - ivan.buyer@gmail.com      (id=7)
 *   - olena.p@ukr.net           (id=8)
 *   - sergiy.tech@outlook.com   (id=9)
 *   - marina.shop@test.com      (id=10)
 *   - Пароль: '12345678'
 */
@Transactional
public class GroupBuySessionControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GroupBuySessionRepository sessionRepository;

  @Autowired
  private GroupMemberRepository memberRepository;

  @AfterEach
  void cleanUp() {
    memberRepository.deleteAll();
    sessionRepository.deleteAll();
  }

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    var result = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
      .andExpect(status().isOk())
      .andReturn();
    jakarta.servlet.http.Cookie c = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", c.getValue());
  }

  /** Створює сесію і повертає її UUID */
  private String createSession(Cookie cookie, long productId) throws Exception {
    String response = mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"productId\":%d}", productId))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();
    return objectMapper.readTree(response).get("uuid").asText();
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/group-buy/sessions/{uuid}
  // ─────────────────────────────────────────────

  @Test
  void getSessionByUuid_ExistingSession_ReturnsDetails() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    String uuid = createSession(cookie, 3L);

    mockMvc.perform(get("/api/v1/group-buy/sessions/" + uuid))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid").value(uuid))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.members").isArray())
      .andExpect(jsonPath("$.currentMembersCount").value(1));
  }

  @Test
  void getSessionByUuid_CompletedSession_IsAvailableFalse() throws Exception {
    // Створюємо сесію з targetSize=3 (product id=1) і заповнюємо її
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("olena.p@ukr.net", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L); // marina створює, стає 1-м учасником
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c2)).andExpect(status().isOk());
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c3)).andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/group-buy/sessions/" + uuid))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.isAvailable").value(false));
  }

  @Test
  void getSessionByUuid_NonExisting_Returns404() throws Exception {
    mockMvc.perform(get("/api/v1/group-buy/sessions/non-existing-uuid-xyz"))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/group-buy/sessions
  // ─────────────────────────────────────────────

  @Test
  void createSession_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":3}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createSession_ValidProduct_ReturnsCreatedSession() throws Exception {
    // Використовуємо apple_auth (merchant) — точно не є учасником жодної сесії
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":3}")
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.uuid").exists())
      .andExpect(jsonPath("$.productId").value(3))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.currentMembersCount").value(1));
  }

  @Test
  void createSession_UserAlreadyInActiveSession_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    // Створюємо першу сесію для product id=2
    createSession(cookie, 2L);

    // Намагаємось створити другу для того самого продукту
    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":2}")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createSession_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":999999}")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void createSession_InitiatorAutomaticallyAddedAsMember() throws Exception {
    Cookie cookie = loginAndGetCookie("olena.p@ukr.net", "12345678");
    String uuid = createSession(cookie, 3L);

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    // User id=8 (olena) має бути в members
    assertTrue(memberRepository.existsBySessionIdAndUserId(session.getId(), 8L));
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/group-buy/sessions/{uuid}/join
  // ─────────────────────────────────────────────

  @Test
  void joinSession_WithoutAuth_Returns401() throws Exception {
    Cookie c = loginAndGetCookie("marina.shop@test.com", "12345678");
    String uuid = createSession(c, 3L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void joinSession_ValidSession_ReturnsSuccess() throws Exception {
    Cookie creatorCookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie joinerCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    String uuid = createSession(creatorCookie, 3L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .cookie(joinerCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.message", containsString("приєдналися")));

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    assertTrue(memberRepository.existsBySessionIdAndUserId(session.getId(), 7L));
  }

  @Test
  void joinSession_AlreadyMember_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    String uuid = createSession(cookie, 3L);

    // marina вже є учасником (як ініціатор) → спроба приєднатись знову
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void joinSession_NonExisting_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions/non-existing-uuid/join")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void joinSession_LastSpot_SessionBecomesCompleted() throws Exception {
    // Product id=1 має targetSize=3
    // Створюємо сесію (1 учасник) → 2 приєднуються → COMPLETED
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c2))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c3))
      .andExpect(status().isOk());

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    assertEquals("COMPLETED", session.getStatus());
  }

  @Test
  void joinSession_CompletedSession_Returns400() throws Exception {
    // Заповнюємо сесію до кінця
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    Cookie c4 = loginAndGetCookie("olena.p@ukr.net", "12345678");

    String uuid = createSession(c1, 1L);
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c2));
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c3));

    // Тепер сесія COMPLETED → c4 намагається приєднатись
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .cookie(c4))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/group-buy/sessions/user
  // ─────────────────────────────────────────────

  @Test
  void getUserSessions_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/group-buy/sessions/user"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserSessions_ReturnsUserSessions() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");
    // Створюємо сесію щоб точно мати хоч одну
    createSession(cookie, 3L);

    mockMvc.perform(get("/api/v1/group-buy/sessions/user").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  void getUserSessions_FilterByActiveStatus_ReturnsOnlyActive() throws Exception {
    Cookie cookie = loginAndGetCookie("nike_distributor@merchant.ua", "12345678");
    createSession(cookie, 3L);

    mockMvc.perform(get("/api/v1/group-buy/sessions/user")
        .param("status", "ACTIVE")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
  }

  @Test
  void getUserSessions_FilterByCompleted_ReturnsOnlyCompleted() throws Exception {
    // Створюємо і заповнюємо сесію до COMPLETED
    Cookie c1 = loginAndGetCookie("olena.p@ukr.net", "12345678");
    Cookie c2 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L);
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c2));
    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join").cookie(c3));

    mockMvc.perform(get("/api/v1/group-buy/sessions/user")
        .param("status", "COMPLETED")
        .cookie(c1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$[*].status", everyItem(is("COMPLETED"))));
  }
}
