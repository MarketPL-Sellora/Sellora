package com.sellora.core.presentation.controllers;

import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Інтеграційні тести для StoreController.
 *
 * Seed-дані:
 *   - User id=1  (ADMIN,    admin@sellora.ua)
 *   - User id=2  (MERCHANT, apple_auth@merchant.ua)       → Store id=1, status=ACTIVE, має products
 *   - User id=3  (MERCHANT, samsung_official@merchant.ua) → Store id=2, status=ACTIVE, має products + активні сесії
 *   - User id=4  (MERCHANT, nike_distributor@merchant.ua) → Store id=3, status=ACTIVE, має products
 *   - User id=5  (MERCHANT, book_island@merchant.ua)      → Store id=4, status=PENDING, НЕ має products
 *   - User id=6  (MERCHANT, coffee_roasters@merchant.ua)  → Store id=5, status=ACTIVE, має product id=4
 *   - User id=7  (BUYER,    ivan.buyer@gmail.com)         → немає магазину
 *   - Пароль для всіх: '12345678'
 *
 * УВАГА щодо порядку тестів:
 *   - Тести що видаляють/змінюють store id=4 можуть конфліктувати між собою
 *   - GET /api/v1/stores вимагає авторизації (закрито Security конфігом)
 */
public class StoreControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StoreRepository storeRepository;

  @Autowired
  private UserRepository userRepository;

  // ─────────────────────────────────────────────
  // ХЕЛПЕРИ
  // ─────────────────────────────────────────────

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    String loginJson = String.format(
      "{\"email\":\"%s\",\"password\":\"%s\"}", email, password
    );
    var result = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginJson))
      .andExpect(status().isOk())
      .andReturn();

    jakarta.servlet.http.Cookie servletCookie = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", servletCookie.getValue());
  }

  private String validCreateStoreJson(String name) {
    return String.format("""
      {
        "name": "%s",
        "address": "м. Київ, вул. Тестова, 1",
        "contactPhone": "+380501234567",
        "description": "Тестовий магазин для інтеграційних тестів"
      }
      """, name);
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/stores
  // ─────────────────────────────────────────────

  @Test
  void getAllStores_WithAuth_ReturnsList() throws Exception {
    // GET /stores вимагає авторизації згідно Security конфігу
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(get("/api/v1/stores").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
  }

  @Test
  void getAllStores_FilterByKeyword_ReturnsMatchingStores() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    // Seed містить "Apple Authorised Reseller"
    mockMvc.perform(get("/api/v1/stores")
        .param("keyword", "Apple")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].name", containsString("Apple")));
  }

  @Test
  void getAllStores_Pagination_ReturnsCorrectPageSize() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(get("/api/v1/stores")
        .param("page", "0")
        .param("size", "2")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.size").value(2));
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/stores/user/{userId}
  // ─────────────────────────────────────────────

  @Test
  void getStoreByUserId_ExistingMerchant_ReturnsStore() throws Exception {
    // User id=2 має Store id=1 (Apple)
    mockMvc.perform(get("/api/v1/stores/user/2"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ownerId").value(2))
      .andExpect(jsonPath("$.name").value("Apple Authorised Reseller"));
  }

  @Test
  void getStoreByUserId_UserWithoutStore_Returns404() throws Exception {
    // User id=7 (Buyer) не має магазину — використовуємо id який точно не має store
    // і не буде створений іншими тестами
    mockMvc.perform(get("/api/v1/stores/user/999"))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/stores/create
  // ─────────────────────────────────────────────

  @Test
  void createStore_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/stores/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateStoreJson("Новий магазин")))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createStore_AsBuyer_ReturnsCreatedAndRoleChangedToMerchant() throws Exception {
    // ivan.buyer@gmail.com (id=7) не має магазину → може створити
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/stores/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateStoreJson("Магазин Івана Унікальний 123"))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.message", containsString("Магазин Івана Унікальний 123")));

    // Роль змінилась на MERCHANT
    var user = userRepository.findById(7L).orElseThrow();
    assertEquals("MERCHANT", user.getRole());

    // Магазин реально є в БД
    assertTrue(storeRepository.findByOwnerId(7L).isPresent());
  }

  @Test
  void createStore_AlreadyHasStore_Returns409() throws Exception {
    // apple_auth@merchant.ua (id=2) вже має магазин → 409
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(post("/api/v1/stores/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateStoreJson("Другий магазин Apple"))
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void createStore_InvalidPhone_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    String json = """
      {
        "name": "Магазин з поганим телефоном",
        "address": "м. Київ, вул. Тестова, 1",
        "contactPhone": "abc",
        "description": "Тест"
      }
      """;

    mockMvc.perform(post("/api/v1/stores/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createStore_AdminCreatesStore_RoleRemainsAdmin() throws Exception {
    // admin@sellora.ua (id=1) не має магазину → може створити, роль не міняється
    Cookie cookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(post("/api/v1/stores/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateStoreJson("Адмін-магазин Унікальний 456"))
        .cookie(cookie))
      .andExpect(status().isCreated());

    // Роль адміна залишилась ADMIN
    var admin = userRepository.findById(1L).orElseThrow();
    assertEquals("ADMIN", admin.getRole());
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/stores/{storeId}
  // ─────────────────────────────────────────────

  @Test
  void updateStore_AsOwner_ReturnsUpdatedStore() throws Exception {
    // apple_auth@merchant.ua → store id=1
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "name": "Apple Reseller Updated",
        "address": "м. Київ, вул. Нова, 99",
        "contactPhone": "+380509999999",
        "description": "Оновлений опис"
      }
      """;

    mockMvc.perform(put("/api/v1/stores/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Apple Reseller Updated"))
      .andExpect(jsonPath("$.address").value("м. Київ, вул. Нова, 99"));
  }

  @Test
  void updateStore_AsOtherMerchant_Returns403() throws Exception {
    // samsung намагається редагувати магазин Apple (store id=1)
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    String json = """
      {
        "name": "Чужий магазин",
        "address": "Адреса",
        "contactPhone": "+380501111111",
        "description": "Опис"
      }
      """;

    mockMvc.perform(put("/api/v1/stores/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateStore_WithDuplicateName_Returns409() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    // Намагаємось назвати магазин як Samsung Official Store (вже існує)
    String json = """
      {
        "name": "Samsung Official Store",
        "address": "Адреса",
        "contactPhone": "+380501234567",
        "description": "Опис"
      }
      """;

    mockMvc.perform(put("/api/v1/stores/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void updateStore_NonExisting_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "name": "Неіснуючий магазин",
        "address": "Адреса",
        "contactPhone": "+380501234567",
        "description": "Опис"
      }
      """;

    mockMvc.perform(put("/api/v1/stores/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // PATCH /api/v1/stores/{storeId}/status
  // ─────────────────────────────────────────────

  @Test
  void updateStoreStatus_AdminActivatesPendingStore_ReturnsActive() throws Exception {
    // Store id=4 (book_island) має статус PENDING → тільки адмін може активувати
    // УВАГА: цей тест не можна запускати після deleteStore_AsAdmin_NoProducts
    Cookie cookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    var store = storeRepository.findById(4L);
    // Якщо store вже видалений іншим тестом — пропускаємо
    if (store.isEmpty()) return;

    mockMvc.perform(patch("/api/v1/stores/4/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void updateStoreStatus_MerchantActivatesPendingOwnStore_Returns403() throws Exception {
    // Власник не може сам активувати PENDING магазин
    // Store id=4 (book_island, owner=user id=5)
    Cookie cookie = loginAndGetCookie("book_island@merchant.ua", "12345678");

    var store = storeRepository.findById(4L);
    if (store.isEmpty()) return;

    mockMvc.perform(patch("/api/v1/stores/4/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateStoreStatus_MerchantBlocksStore_Returns403() throws Exception {
    // Merchant не може заблокувати магазин
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(patch("/api/v1/stores/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"BLOCKED\"}")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateStoreStatus_OwnerClosesStore_WithActiveSessions_Returns409() throws Exception {
    // Store id=1 (Apple) має активні group_buy_sessions → не можна закрити
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(patch("/api/v1/stores/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"CLOSED\"}")
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void updateStoreStatus_OtherMerchantClosesStore_Returns403() throws Exception {
    // Samsung намагається закрити магазин Apple
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    mockMvc.perform(patch("/api/v1/stores/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"CLOSED\"}")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateStoreStatus_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(patch("/api/v1/stores/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}"))
      .andExpect(status().isUnauthorized());
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/stores/{storeId}
  // ─────────────────────────────────────────────

  @Test
  void deleteStore_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/stores/3"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteStore_AsOwner_WithProducts_Returns409() throws Exception {
    // Store id=1 (Apple) має products → не можна видалити
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/stores/1")
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void deleteStore_AsOtherMerchant_Returns403() throws Exception {
    // Samsung намагається видалити магазин Apple
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/stores/1")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void deleteStore_AsAdmin_WithProducts_Returns409() throws Exception {
    // Навіть адмін не може видалити магазин з товарами
    Cookie cookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(delete("/api/v1/stores/1")
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void deleteStore_AsAdmin_NoProducts_Returns204() throws Exception {
    // Store id=4 (book_island) не має products → адмін може видалити
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    var store = storeRepository.findById(4L);
    if (store.isEmpty()) return; // вже видалено іншим тестом

    mockMvc.perform(delete("/api/v1/stores/4")
        .cookie(adminCookie))
      .andExpect(status().isNoContent());

    assertFalse(storeRepository.findById(4L).isPresent());
  }
}
