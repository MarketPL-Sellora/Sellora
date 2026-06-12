package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.infrastructure.persistence.CartItemRepository;
import com.sellora.core.infrastructure.persistence.CartRepository;
import com.sellora.core.infrastructure.persistence.OrderRepository;
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
 * Інтеграційні тести для OrderController.
 *
 * Актуальні Seed-дані (з ecommerce-seed-data-v2):
 * - Store id=1 (Apple)   → products id=1 (iPhone), id=2 (MacBook)
 * - Store id=2 (Samsung) → products id=3 (Galaxy S24), id=4 (Watch 6)
 * - Store id=3 (Nike)    → products id=5 (Air Force 1)
 * - Store id=5 (Coffee)  → products id=10 (Кава Ефіопія)
 *
 * СТРАТЕГІЯ: Кожен тест сам додає товари в кошик через API перед checkout.
 */
public class OrderControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  // ─────────────────────────────────────────────
  // ХЕЛПЕРИ
  // ─────────────────────────────────────────────

  private Cookie loginAndGetCookie(String email, String password) throws Exception {
    var result = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
      .andExpect(status().isOk())
      .andReturn();
    jakarta.servlet.http.Cookie c = result.getResponse().getCookie("accessToken");
    return new Cookie("accessToken", c.getValue());
  }

  private void addToCart(Cookie cookie, long productId, int qty) throws Exception {
    mockMvc.perform(post("/api/v1/cart")
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"productId\":%d,\"quantity\":%d}", productId, qty))
        .cookie(cookie))
      .andExpect(status().isOk());
  }

  private String checkoutJson(long storeId, long productId, int qty) {
    return String.format("""
      {
        "store_id": %d,
        "buyer_name": "Іван",
        "buyer_surname": "Тестовий",
        "buyer_phone": "+380501234567",
        "buyer_email": "test@example.com",
        "delivery_type": "PICKUP",
        "payment_method": "CASH_ON_DELIVERY",
        "items": [{"product_id": %d, "quantity": %d}]
      }
      """, storeId, productId, qty);
  }

  private String checkoutJsonWithBranch(long storeId, long productId, int qty) {
    return String.format("""
      {
        "store_id": %d,
        "buyer_name": "Іван",
        "buyer_surname": "Тестовий",
        "buyer_phone": "+380501234567",
        "buyer_email": "test@example.com",
        "delivery_type": "BRANCH",
        "carrier_id": 1,
        "delivery_address": {"city": "Київ", "street": "Тестова 1"},
        "payment_method": "CASH_ON_DELIVERY",
        "items": [{"product_id": %d, "quantity": %d}]
      }
      """, storeId, productId, qty);
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/orders/cart/checkout
  // ─────────────────────────────────────────────

  @Test
  void checkout_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void checkout_ValidRequest_ReturnsCreatedOrder() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1); // Store 1, Product 1

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.purchase_type").value("REGULAR"))
      .andExpect(jsonPath("$.payment_status").value("PENDING"))
      .andExpect(jsonPath("$.shipping_status").value("PENDING"))
      .andExpect(jsonPath("$.items").isArray())
      .andExpect(jsonPath("$.items", hasSize(1)))
      .andExpect(jsonPath("$.subtotal").value(greaterThan(0.0)))
      .andExpect(jsonPath("$.total_amount").value(greaterThan(0.0)));
  }

  @Test
  void checkout_ValidRequest_StockDecreased() throws Exception {
    Cookie cookie = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 3L, 2); // Samsung Galaxy (Store 2)

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(2L, 3L, 2)) // Виправляємо на Store 2
        .cookie(cookie))
      .andExpect(status().isCreated());

    var product = mockMvc.perform(get("/api/v1/products/3"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    int stockAfter = objectMapper.readTree(product).get("stockQuantity").asInt();
    assertTrue(stockAfter <= 98);
  }

  @Test
  void checkout_ValidRequest_CartItemsRemovedAfterCheckout() throws Exception {
    Cookie cookie = loginAndGetCookie("olena.p@ukr.net", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1); // Store 1

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isCreated());

    var cart = cartRepository.findByUserId(8L);
    if (cart.isPresent()) {
      var items = cartItemRepository.findByCartId(cart.get().getId());
      assertTrue(items.stream().noneMatch(i -> i.getProduct().getId().equals(1L)));
    }
  }

  @Test
  void checkout_WithTaxCalculation_TotalCorrect() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 10L, 1); // Кава Ефіопія (Store 5)

    String response = mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(5L, 10L, 1)) // Store 5
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andReturn().getResponse().getContentAsString();

    var json = objectMapper.readTree(response);
    double subtotal = json.get("subtotal").asDouble();
    double tax = json.get("tax").asDouble();
    double total = json.get("total_amount").asDouble();

    assertEquals(subtotal * 0.01, tax, 0.01);
    assertEquals(subtotal + tax, total, 0.01);
  }

  @Test
  void checkout_EmptyCart_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void checkout_StoreNotFound_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1);

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(999999L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isNotFound());
  }

  @Test
  void checkout_ProductFromWrongStore_Returns409() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));

    addToCart(cookie, 3L, 1); // Product 3 (Samsung) -> Store 2

    // Спроба купити Samsung через магазин Apple (Store 1)
    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 3L, 1))
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void checkout_ProductNotInCart_Returns409() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 2L, 1); // В кошику товар 2

    String json = """
      {
        "store_id": 1,
        "buyer_name": "Іван",
        "buyer_surname": "Тест",
        "buyer_phone": "+380501234567",
        "buyer_email": "test@example.com",
        "delivery_type": "PICKUP",
        "payment_method": "CASH_ON_DELIVERY",
        "items": [{"product_id": 1, "quantity": 1}]
      }
      """;

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isConflict());
  }

  @Test
  void checkout_BranchDelivery_WithoutCarrierId_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1);

    String json = """
      {
        "store_id": 1,
        "buyer_name": "Іван",
        "buyer_surname": "Тест",
        "buyer_phone": "+380501234567",
        "buyer_email": "test@example.com",
        "delivery_type": "BRANCH",
        "payment_method": "CASH_ON_DELIVERY",
        "items": [{"product_id": 1, "quantity": 1}]
      }
      """;

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  @Test
  void checkout_BranchDelivery_WithCarrierAndAddress_ReturnsCreated() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1);

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJsonWithBranch(1L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.delivery_type").value("BRANCH"));
  }

  @Test
  void checkout_MissingRequiredFields_Returns400() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    String json = """
      {
        "store_id": 1,
        "buyer_surname": "Тест",
        "buyer_phone": "+380501234567",
        "buyer_email": "test@example.com",
        "delivery_type": "PICKUP",
        "payment_method": "CASH_ON_DELIVERY",
        "items": [{"product_id": 1, "quantity": 1}]
      }
      """;

    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .cookie(cookie))
      .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/orders
  // ─────────────────────────────────────────────

  @Test
  void getUserOrderHistory_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/orders"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserOrderHistory_WithOrders_ReturnsList() throws Exception {
    Cookie cookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 1L, 1);
    mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1))
        .cookie(cookie))
      .andExpect(status().isCreated());

    mockMvc.perform(get("/api/v1/orders").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
  }

  @Test
  void getUserOrderHistory_NoOrders_ReturnsEmptyPage() throws Exception {
    Cookie cookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(get("/api/v1/orders").cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.totalElements").value(0));
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/orders/{orderId}
  // ─────────────────────────────────────────────

  @Test
  void getOrderDetails_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(get("/api/v1/orders/1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void getOrderDetails_AsBuyer_ReturnsOrder() throws Exception {
    Cookie cookie = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(cookie));
    addToCart(cookie, 3L, 1);

    String createResponse = mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(2L, 3L, 1)) // Store 2
        .cookie(cookie))
      .andExpect(status().isCreated())
      .andReturn().getResponse().getContentAsString();

    Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

    mockMvc.perform(get("/api/v1/orders/" + orderId).cookie(cookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(orderId))
      .andExpect(jsonPath("$.items").isArray())
      .andExpect(jsonPath("$.items", hasSize(1)));
  }

  @Test
  void getOrderDetails_AsStoreOwner_ReturnsOrder() throws Exception {
    Cookie buyerCookie = loginAndGetCookie("olena.p@ukr.net", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(buyerCookie));
    addToCart(buyerCookie, 1L, 1);

    String createResponse = mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(1L, 1L, 1))
        .cookie(buyerCookie))
      .andExpect(status().isCreated())
      .andReturn().getResponse().getContentAsString();

    Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

    Cookie merchantCookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");

    mockMvc.perform(get("/api/v1/orders/" + orderId).cookie(merchantCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(orderId));
  }

  @Test
  void getOrderDetails_AsOtherUser_Returns403() throws Exception {
    Cookie marinaCookie = loginAndGetCookie("marina.shop@test.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(marinaCookie));
    addToCart(marinaCookie, 10L, 1);

    String createResponse = mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(5L, 10L, 1)) // Store 5
        .cookie(marinaCookie))
      .andExpect(status().isCreated())
      .andReturn().getResponse().getContentAsString();

    Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

    Cookie ivanCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(get("/api/v1/orders/" + orderId).cookie(ivanCookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void getOrderDetails_AsAdmin_ReturnsOrder() throws Exception {
    Cookie sergiyCookie = loginAndGetCookie("sergiy.tech@outlook.com", "12345678");
    mockMvc.perform(delete("/api/v1/cart").cookie(sergiyCookie));
    addToCart(sergiyCookie, 10L, 1);

    String createResponse = mockMvc.perform(post("/api/v1/orders/cart/checkout")
        .contentType(MediaType.APPLICATION_JSON)
        .content(checkoutJson(5L, 10L, 1)) // Store 5
        .cookie(sergiyCookie))
      .andExpect(status().isCreated())
      .andReturn().getResponse().getContentAsString();

    Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(get("/api/v1/orders/" + orderId).cookie(adminCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(orderId));
  }

  @Test
  void getOrderDetails_NonExisting_Returns404() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(get("/api/v1/orders/999999").cookie(cookie))
      .andExpect(status().isNotFound());
  }
}
