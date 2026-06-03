package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProductControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProductRepository productRepository;

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

    // ✅ Повертаємо реальний Cookie об'єкт — MockMvc передасть його через request.getCookies()
    jakarta.servlet.http.Cookie servletCookie = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", servletCookie.getValue());
  }

  private String validCreateProductJson(Long categoryId) {
    return String.format("""
      {
        "title": "Тестовий товар",
        "description": "Це тестовий опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": %d,
        "standardPrice": 1000.00,
        "groupPrice": 800.00,
        "groupTargetSize": 3,
        "stockQuantity": 10,
        "images": [],
        "attributes": {}
      }
      """, categoryId);
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/products
  // ─────────────────────────────────────────────

  @Test
  void getProducts_NoAuth_ReturnsPublicList() throws Exception {
    mockMvc.perform(get("/api/v1/products"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
  }

  @Test
  void getProducts_FilterByKeyword_ReturnsMatchingProducts() throws Exception {
    mockMvc.perform(get("/api/v1/products")
        .param("keyword", "iPhone"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].title", containsString("iPhone")));
  }

  @Test
  void getProducts_FilterByMinPrice_ReturnsCorrectProducts() throws Exception {
    mockMvc.perform(get("/api/v1/products")
        .param("minPrice", "40000"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[*].standardPrice",
        everyItem(greaterThanOrEqualTo(40000.0))));
  }

  @Test
  void getProducts_OnlyFavorites_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/products")
        .param("onlyFavorites", "true"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getProducts_OnlyFavorites_WithAuth_ReturnsFavorites() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(get("/api/v1/products")
        .param("onlyFavorites", "true")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[*].isFavorite", everyItem(is(true))));
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/products/{id}
  // ─────────────────────────────────────────────

  @Test
  void getProductById_ExistingProduct_ReturnsProduct() throws Exception {
    mockMvc.perform(get("/api/v1/products/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(1))
      .andExpect(jsonPath("$.title").isNotEmpty());
  }

  @Test
  void getProductById_WithFullFlag_ReturnsCategoryAndStoreName() throws Exception {
    mockMvc.perform(get("/api/v1/products/1")
        .param("full", "true"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.categoryName").isNotEmpty())
      .andExpect(jsonPath("$.storeName").isNotEmpty());
  }

  @Test
  void getProductById_NonExistingProduct_Returns404() throws Exception {
    mockMvc.perform(get("/api/v1/products/999999"))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/products/merchant/{merchantId}
  // ─────────────────────────────────────────────

  @Test
  void getProductsByMerchant_ExistingMerchant_ReturnsProducts() throws Exception {
    mockMvc.perform(get("/api/v1/products/merchant/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
      .andExpect(jsonPath("$.content[*].merchantId", everyItem(is(1))));
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/products
  // ─────────────────────────────────────────────

  @Test
  void createProduct_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateProductJson(2L)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createProduct_AsMerchant_ReturnsCreated() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");
    long countBefore = productRepository.count();

    mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateProductJson(2L))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.title").value("Тестовий товар"))
      .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertEquals(countBefore + 1, productRepository.count());
  }

  @Test
  void createProduct_StockZero_StatusIsOutOfStock() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "title": "Товар без запасів",
        "description": "Це тестовий опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": 2,
        "standardPrice": 500.00,
        "groupPrice": 400.00,
        "groupTargetSize": 2,
        "stockQuantity": 0,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
  }

  @Test
  void createProduct_GroupPriceHigherThanStandard_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "title": "Невалідний товар",
        "description": "Це тестовий опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": 2,
        "standardPrice": 800.00,
        "groupPrice": 900.00,
        "groupTargetSize": 3,
        "stockQuantity": 5,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createProduct_AsBuyer_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateProductJson(2L))
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/products/{id}
  // ─────────────────────────────────────────────

  @Test
  void updateProduct_AsOwner_ReturnsUpdatedProduct() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "title": "iPhone 15 Pro ОНОВЛЕНИЙ",
        "description": "Це оновлений опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": 2,
        "standardPrice": 49000.00,
        "groupPrice": 45000.00,
        "groupTargetSize": 3,
        "stockQuantity": 45,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(put("/api/v1/products/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.title").value("iPhone 15 Pro ОНОВЛЕНИЙ"))
      .andExpect(jsonPath("$.standardPrice").value(49000.00));
  }

  @Test
  void updateProduct_AsOtherMerchant_Returns403() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    String json = """
      {
        "title": "Чужий товар",
        "description": "Це тестовий опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": 2,
        "standardPrice": 1000.00,
        "groupPrice": 800.00,
        "groupTargetSize": 3,
        "stockQuantity": 10,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(put("/api/v1/products/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void updateProduct_NonExisting_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String json = """
      {
        "title": "Неіснуючий товар",
        "description": "Це тестовий опис товару що містить щонайменше п'ятдесят символів для валідації",
        "categoryId": 2,
        "standardPrice": 1000.00,
        "groupPrice": 800.00,
        "groupTargetSize": 3,
        "stockQuantity": 10,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(put("/api/v1/products/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // PATCH /api/v1/products/{id}/status
  // ─────────────────────────────────────────────

  @Test
  void updateStatus_ToArchived_ReturnsArchivedProduct() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(patch("/api/v1/products/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ARCHIVED\"}")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ARCHIVED"));
  }

  @Test
  void updateStatus_ActivateWithZeroStock_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("coffee_roasters@merchant.ua", "12345678");

    // Архівуємо product id=4
    mockMvc.perform(patch("/api/v1/products/4/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ARCHIVED\"}")
        .cookie(cookie))
      .andExpect(status().isOk());

    // Обнуляємо stock
    String updateJson = """
      {
        "title": "Кавомолка електрична",
        "description": "Жорнова кавомолка з нержавіючої сталі преміум класу для справжніх поціновувачів кави",
        "categoryId": 1,
        "standardPrice": 3200.00,
        "groupPrice": 2800.00,
        "groupTargetSize": 2,
        "stockQuantity": 0,
        "images": [],
        "attributes": {}
      }
      """;

    mockMvc.perform(put("/api/v1/products/4")
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateJson)
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));

    // Активуємо з 0 stock — має бути 400
    mockMvc.perform(patch("/api/v1/products/4/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void updateStatus_AsOtherMerchant_Returns403() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    mockMvc.perform(patch("/api/v1/products/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ARCHIVED\"}")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/products/{id}
  // ─────────────────────────────────────────────

  @Test
  void deleteProduct_AsOwner_Returns204() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    String createResult = mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validCreateProductJson(2L))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andReturn()
      .getResponse()
      .getContentAsString();

    Long newProductId = objectMapper.readTree(createResult).get("id").asLong();

    mockMvc.perform(delete("/api/v1/products/" + newProductId)
        .cookie(cookie))
      .andExpect(status().isNoContent());

    assertFalse(productRepository.findById(newProductId).isPresent());
  }

  @Test
  void deleteProduct_AsOtherMerchant_Returns403() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/products/1")
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void deleteProduct_WithActiveSession_Returns409() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/products/1")
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void deleteProduct_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/products/1"))
      .andExpect(status().isUnauthorized());
  }
}
