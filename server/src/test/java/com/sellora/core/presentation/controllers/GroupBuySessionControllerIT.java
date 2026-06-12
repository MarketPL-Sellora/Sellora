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
 * - Product id=1 → ACTIVE, groupTargetSize=3, groupPrice=44000
 * - Product id=2 → ACTIVE, groupTargetSize=3, groupPrice=47000
 * - Product id=3 → ACTIVE, groupTargetSize=5, groupPrice=4800
 *
 * Users:
 * - ivan.buyer@gmail.com      (id=7)
 * - olena.p@ukr.net           (id=8)
 * - sergiy.tech@outlook.com   (id=9)
 * - marina.shop@test.com      (id=10)
 * - Пароль: '12345678'
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

  private String validCheckoutRequestJson(long productId) {
    return String.format("""
        {
          "buyer_name": "Тест",
          "buyer_surname": "Тестов",
          "buyer_phone": "+380501112233",
          "buyer_email": "test@example.com",
          "delivery_type": "PICKUP",
          "payment_method": "CASH_ON_DELIVERY",
          "product_id": %d,
          "quantity": 1
        }
        """, productId);
  }

  /** Створює сесію і повертає її UUID */
  private String createSession(Cookie cookie, long productId) throws Exception {
    String response = mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(productId))
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
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("olena.p@ukr.net", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c2)).andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c3)).andExpect(status().isOk());

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
        .content(validCheckoutRequestJson(3L)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createSession_ValidProduct_ReturnsCreatedSession() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(3L))
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
    createSession(cookie, 2L);

    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(2L))
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createSession_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(999999L))
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void createSession_InitiatorAutomaticallyAddedAsMember() throws Exception {
    Cookie cookie = loginAndGetCookie("olena.p@ukr.net", "12345678");
    String uuid = createSession(cookie, 3L);

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    assertTrue(memberRepository.existsBySessionIdAndUserId(session.getId(), 8L));
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/group-buy/sessions/{uuid}/join
  // ─────────────────────────────────────────────

  @Test
  void joinSession_WithoutAuth_Returns401() throws Exception {
    Cookie c = loginAndGetCookie("marina.shop@test.com", "12345678");
    String uuid = createSession(c, 3L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(3L)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void joinSession_ValidSession_ReturnsSuccess() throws Exception {
    Cookie creatorCookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie joinerCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    String uuid = createSession(creatorCookie, 3L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(3L))
        .cookie(joinerCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uuid").value(uuid))
      .andExpect(jsonPath("$.currentMembersCount").value(greaterThanOrEqualTo(2)));

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    assertTrue(memberRepository.existsBySessionIdAndUserId(session.getId(), 7L));
  }

  @Test
  void joinSession_AlreadyMember_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    String uuid = createSession(cookie, 3L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(3L))
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void joinSession_NonExisting_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");

    mockMvc.perform(post("/api/v1/group-buy/sessions/non-existing-uuid/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(3L))
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void joinSession_LastSpot_SessionBecomesCompleted() throws Exception {
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(1L))
        .cookie(c2))
      .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(1L))
        .cookie(c3))
      .andExpect(status().isOk());

    var session = sessionRepository.findByUuid(uuid).orElseThrow();
    assertEquals("COMPLETED", session.getStatus());
  }

  @Test
  void joinSession_CompletedSession_Returns400() throws Exception {
    Cookie c1 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c2 = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    Cookie c4 = loginAndGetCookie("olena.p@ukr.net", "12345678");

    String uuid = createSession(c1, 1L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c2));

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c3));

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCheckoutRequestJson(1L))
        .cookie(c4))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/group-buy/sessions/user
  // ─────────────────────────────────────────────

  @Test
  void getUserSessions_ReturnsUserSessions() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");
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
    Cookie c1 = loginAndGetCookie("olena.p@ukr.net", "12345678");
    Cookie c2 = loginAndGetCookie("marina.shop@test.com", "12345678");
    Cookie c3 = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");

    String uuid = createSession(c1, 1L);

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c2));

    mockMvc.perform(post("/api/v1/group-buy/sessions/" + uuid + "/join")
      .contentType(MediaType.APPLICATION_JSON)
      .content(validCheckoutRequestJson(1L))
      .cookie(c3));

    mockMvc.perform(get("/api/v1/group-buy/sessions/user")
        .param("status", "COMPLETED")
        .cookie(c1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$[*].status", everyItem(is("COMPLETED"))));
  }
}
