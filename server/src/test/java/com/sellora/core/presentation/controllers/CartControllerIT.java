package com.sellora.core.presentation.controllers;

import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Інтеграційні тести для CartController.
 *
 * СТРАТЕГІЯ: Кожен тест сам додає потрібні товари в кошик через API,
 * а не покладається на seed-дані кошика (які можуть бути змінені іншими тестами).
 *
 * Seed products (стабільні):
 *   - Product id=1 → ACTIVE, stock=50, standardPrice=48000
 *   - Product id=2 → ACTIVE, stock=30
 *   - Product id=3 → ACTIVE, stock=100
 *
 * Users:
 *   - ivan.buyer@gmail.com   (id=7)
 *   - olena.p@ukr.net        (id=8)
 *   - marina.shop@test.com   (id=10) — використовуємо для тестів щоб не конфліктувати
 *   - Пароль: '12345678'
 */
public class CartControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    var result = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
      .andExpect(status().isOk())
      .andReturn();
    jakarta.servlet.http.Cookie c = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", c.getValue());
  }

  /** Допоміжний метод — додає товар в кошик і повертає */
  private void addItemToCart(Cookie cookie, long productId, int quantity) throws Exception {
    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"productId\":%d,\"quantity\":%d}", productId, quantity))
        .cookie(cookie))
      .andExpect(status().isOk());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/cart
  // ─────────────────────────────────────────────

  @Test
  void getCart_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/cart"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getCart_WithItems_ReturnsCartWithItems() throws Exception {
    Cookie cookie = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    // Спочатку очищаємо кошик і додаємо свіжий товар
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addItemToCart(cookie, 1L, 2);

    mockMvc.perform(get("/api/v1/cart").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items").isArray())
      .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
      .andExpect(jsonPath("$.totalAmount").value(greaterThan(0.0)));
  }

  @Test
  void getCart_EmptyCart_ReturnsEmptyItems() throws Exception {
    // Використовуємо apple merchant — у нього точно немає кошика
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");
    // Очищаємо на всяк випадок
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));

    mockMvc.perform(get("/api/v1/cart").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items").isArray())
      .andExpect(jsonPath("$.items", hasSize(0)))
      .andExpect(jsonPath("$.totalAmount").value(0));
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/cart
  // ─────────────────────────────────────────────

  @Test
  void addToCart_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":1}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void addToCart_NewProduct_ReturnsSuccess() throws Exception {
    Cookie cookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));

    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":2}")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.productId").value(1))
      .andExpect(jsonPath("$.quantity").value(2))
      .andExpect(jsonPath("$.subTotal").value(greaterThan(0.0)))
      .andExpect(jsonPath("$.totalAmount").value(greaterThan(0.0)));
  }

  @Test
  void addToCart_ExistingProduct_IncreasesQuantity() throws Exception {
    Cookie cookie = loginAndGetCookie("nike_distributor@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    // Додаємо 1 одиницю
    addItemToCart(cookie, 2L, 1);

    // Додаємо ще 1 → очікуємо quantity=2
    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":2,\"quantity\":1}")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.productId").value(2))
      .andExpect(jsonPath("$.quantity").value(2));
  }

  @Test
  void addToCart_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":999999,\"quantity\":1}")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void addToCart_ExceedsStock_Returns400() throws Exception {
    // Product id=1 має stock=50 → намагаємось додати 100
    Cookie cookie = loginAndGetCookie("book_island@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));

    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":100}")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void addToCart_InvalidQuantityZero_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":0}")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // PATCH /api/v1/cart/{productId}/quantity
  // ─────────────────────────────────────────────

  @Test
  void updateQuantity_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(patch("/api/v1/cart/1/quantity")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"new_quantity\":3}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void updateQuantity_ExistingItem_ReturnsUpdated() throws Exception {
    Cookie cookie = loginAndGetCookie("coffee_roasters@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addItemToCart(cookie, 1L, 1);

    mockMvc.perform(patch("/api/v1/cart/1/quantity")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"new_quantity\":5}")
        .cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.productId").value(1))
      .andExpect(jsonPath("$.quantity").value(5));
  }

  @Test
  void updateQuantity_ProductNotInCart_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("coffee_roasters@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    // Не додаємо product id=5 → має бути 404

    mockMvc.perform(patch("/api/v1/cart/5/quantity")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"new_quantity\":1}")
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void updateQuantity_ExceedsStock_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("coffee_roasters@merchant.ua", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addItemToCart(cookie, 1L, 1);

    // Product id=1 має stock=50 → встановлюємо 999
    mockMvc.perform(patch("/api/v1/cart/1/quantity")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"new_quantity\":999}")
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/cart/{productId}
  // ─────────────────────────────────────────────

  @Test
  void removeItem_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/cart/1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void removeItem_ExistingItem_Returns204() throws Exception {
    Cookie cookie = loginAndGetCookie("olena.p@ukr.net", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addItemToCart(cookie, 3L, 1);

    mockMvc.perform(delete("/api/v1/cart/3").cookie(cookie))
      .andExpect(status().isNoContent());

    // Перевіряємо що товар видалено
    var cart = cartRepository.findByUserId(8L).orElseThrow();
    var items = cartItemRepository.findByCartId(cart.getId());
    assertTrue(items.stream().noneMatch(i -> i.getProduct().getId().equals(3L)));
  }

  @Test
  void removeItem_ProductNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("olena.p@ukr.net", "12345678");

    mockMvc.perform(delete("/api/v1/cart/999999").cookie(cookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/cart
  // ─────────────────────────────────────────────

  @Test
  void clearCart_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/cart"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void clearCart_WithItems_Returns204AndCartIsEmpty() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    addItemToCart(cookie, 2L, 1);

    mockMvc.perform(delete("/api/v1/cart").cookie(cookie))
      .andExpect(status().isNoContent());

    // Кошик тепер порожній
    var cart = cartRepository.findByUserId(7L).orElseThrow();
    assertTrue(cartItemRepository.findByCartId(cart.getId()).isEmpty());
  }
}
