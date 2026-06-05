package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.ShippingCarrier;
import com.sellora.core.infrastructure.persistence.ShippingCarrierRepository;
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
public class ShippingCarrierControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ShippingCarrierRepository shippingCarrierRepository;

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

  private String createCarrierJson(String name, String code, boolean isActive) {
    return String.format("""
      {
        "name": "%s",
        "code": "%s",
        "is_active": %b
      }
      """, name, code, isActive);
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/shipping_carriers
  // ─────────────────────────────────────────────

  @Test
  void createCarrier_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/shipping_carriers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Нова Пошта", "NP", true)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createCarrier_AsBuyer_Returns403() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/shipping_carriers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Нова Пошта", "NP", true))
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void createCarrier_AsAdmin_Returns201() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");
    long countBefore = shippingCarrierRepository.count();

    mockMvc.perform(post("/api/v1/shipping_carriers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Meest Express", "MEEST", true))
        .cookie(adminCookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.name").value("Meest Express"))
      .andExpect(jsonPath("$.code").value("MEEST"))
      .andExpect(jsonPath("$.is_active").value(true));

    assertEquals(countBefore + 1, shippingCarrierRepository.count());
    assertTrue(shippingCarrierRepository.existsByCode("MEEST"));
  }

  @Test
  void createCarrier_DuplicateCode_Returns409() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(post("/api/v1/shipping_carriers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Укрпошта", "UKR_POSHTA", true))
        .cookie(adminCookie))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/shipping_carriers")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Інша Пошта", "UKR_POSHTA", true))
        .cookie(adminCookie))
      .andExpect(status().isConflict());
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/shipping_carriers/{id}
  // ─────────────────────────────────────────────

  @Test
  void updateCarrier_AsAdmin_Returns200() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    ShippingCarrier carrier = new ShippingCarrier();
    carrier.setName("Стара Пошта");
    carrier.setCode("OLD_P");
    carrier.setIsActive(true);
    shippingCarrierRepository.save(carrier);

    mockMvc.perform(put("/api/v1/shipping_carriers/" + carrier.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Оновлена Пошта", "UPD_P", false))
        .cookie(adminCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Оновлена Пошта"))
      .andExpect(jsonPath("$.code").value("UPD_P"))
      .andExpect(jsonPath("$.is_active").value(false));
  }

  @Test
  void updateCarrier_DuplicateCode_Returns409() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    ShippingCarrier carrier1 = new ShippingCarrier();
    carrier1.setName("Служба 1");
    carrier1.setCode("C_ONE");
    carrier1.setIsActive(true);
    shippingCarrierRepository.save(carrier1);

    ShippingCarrier carrier2 = new ShippingCarrier();
    carrier2.setName("Служба 2");
    carrier2.setCode("C_TWO");
    carrier2.setIsActive(true);
    shippingCarrierRepository.save(carrier2);

    mockMvc.perform(put("/api/v1/shipping_carriers/" + carrier2.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Служба 2 Оновлена", "C_ONE", true))
        .cookie(adminCookie))
      .andExpect(status().isConflict());
  }

  @Test
  void updateCarrier_NonExisting_Returns404() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(put("/api/v1/shipping_carriers/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createCarrierJson("Неіснуюча", "N_EXT", true))
        .cookie(adminCookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/shipping_carriers
  // ─────────────────────────────────────────────

  @Test
  void getAllCarriers_ReturnsList() throws Exception {
    Cookie buyerCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    ShippingCarrier carrier = new ShippingCarrier();
    carrier.setName("Auth Carrier");
    carrier.setCode("AUTH_C");
    carrier.setIsActive(true);
    shippingCarrierRepository.save(carrier);

    mockMvc.perform(get("/api/v1/shipping_carriers")
        .cookie(buyerCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
  }

  @Test
  void getCarrierById_Existing_ReturnsCarrier() throws Exception {
    Cookie buyerCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    ShippingCarrier carrier = new ShippingCarrier();
    carrier.setName("Specific Carrier");
    carrier.setCode("SPEC_C");
    carrier.setIsActive(true);
    shippingCarrierRepository.save(carrier);

    mockMvc.perform(get("/api/v1/shipping_carriers/" + carrier.getId())
        .cookie(buyerCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Specific Carrier"))
      .andExpect(jsonPath("$.code").value("SPEC_C"));
  }

  @Test
  void getCarrierById_NonExisting_Returns404() throws Exception {
    Cookie buyerCookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(get("/api/v1/shipping_carriers/999999")
        .cookie(buyerCookie))
      .andExpect(status().isNotFound());
  }
}
