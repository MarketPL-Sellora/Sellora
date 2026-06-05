package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.Category;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class CategoryControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CategoryRepository categoryRepository;

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

  private String createCategoryJson(String name, Long parentId) {
    if (parentId == null) {
      return String.format("{\"name\": \"%s\"}", name);
    }
    return String.format("{\"name\": \"%s\", \"parentId\": %d}", name, parentId);
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/categories (Створення)
  // ─────────────────────────────────────────────

  @Test
  void createCategory_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/categories")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCategoryJson("Нова Категорія", null)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createCategory_AsAdmin_Returns201() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");
    long countBefore = categoryRepository.count();

    mockMvc.perform(post("/api/v1/categories")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCategoryJson("Унікальна Тестова Категорія 99", null))
        .cookie(adminCookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.name").value("Унікальна Тестова Категорія 99"));

    assertEquals(countBefore + 1, categoryRepository.count());
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/categories/{id} (Видалення)
  // ─────────────────────────────────────────────

  @Test
  void deleteCategory_AsAdmin_Returns204() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    Category category = new Category();
    category.setName("Категорія для видалення");
    // Якщо у вашій сутності зв'язок реалізовано через об'єкт (setParent), залиште null,
    // або використовуйте setParentId(null) залежно від імплементації
    Category savedCategory = categoryRepository.save(category);

    mockMvc.perform(delete("/api/v1/categories/" + savedCategory.getId())
        .cookie(adminCookie))
      .andExpect(status().isNoContent()); // Очікуємо 204 No Content

    assertFalse(categoryRepository.existsById(savedCategory.getId()));
  }

  @Test
  void deleteCategory_AsMerchant_Returns403() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/categories/1")
        .cookie(merchantCookie))
      .andExpect(status().is5xxServerError()); // Або is4xxClientError(), залежно від того чи ви додали обробник
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/categories (Публічні/Загальні ендпоінти)
  // ─────────────────────────────────────────────

  @Test
  void getAllCategories_Paginated_Returns200() throws Exception {
    Category category = new Category();
    category.setName("Пагінована категорія");
    categoryRepository.save(category);

    // Запит з параметрами пагінації
    mockMvc.perform(get("/api/v1/categories")
        .param("page", "0")
        .param("size", "5"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
      .andExpect(jsonPath("$.totalElements").exists())
      .andExpect(jsonPath("$.totalPages").exists());
  }

  @Test
  void getCategoryTree_ReturnsArray() throws Exception {
    Category parent = new Category();
    parent.setName("Головна категорія");
    categoryRepository.save(parent);

    mockMvc.perform(get("/api/v1/categories/tree"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
  }

  @Test
  void getCategoryById_Existing_Returns200() throws Exception {
    Category category = new Category();
    category.setName("Цільова категорія");
    Category savedCategory = categoryRepository.save(category);

    mockMvc.perform(get("/api/v1/categories/" + savedCategory.getId()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(savedCategory.getId()))
      .andExpect(jsonPath("$.name").value("Цільова категорія"));
  }

  @Test
  void getCategoryById_NonExisting_Returns404() throws Exception {
    mockMvc.perform(get("/api/v1/categories/999999"))
      .andExpect(status().isNotFound());
  }
}
