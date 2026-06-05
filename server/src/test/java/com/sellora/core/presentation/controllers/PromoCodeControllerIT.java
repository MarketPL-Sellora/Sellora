package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.PromoCode;
import com.sellora.core.infrastructure.persistence.PromoCodeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class PromoCodeControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PromoCodeRepository promoCodeRepository;

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

  private String createPromoCodeJson(String code, String discountType, double value, boolean isActive) {
    return String.format("""
      {
        "code": "%s",
        "discount_type": "%s",
        "value": %s,
        "is_active": %b,
        "start_date": "2026-06-01T00:00:00Z",
        "end_date": "2026-12-31T23:59:59Z",
        "usage_limit": 100
      }
      """, code, discountType, value, isActive);
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/promo_codes
  // ─────────────────────────────────────────────

  @Test
  void createPromoCode_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/promo_codes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("SUMMER20", "PERCENTAGE", 20.0, true)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createPromoCode_AsBuyer_Returns403() throws Exception {
    Cookie cookie = loginAndGetCookie("ivan.buyer@gmail.com", "12345678");

    mockMvc.perform(post("/api/v1/promo_codes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("SUMMER20", "PERCENTAGE", 20.0, true))
        .cookie(cookie))
      .andExpect(status().isForbidden());
  }

  @Test
  void createPromoCode_AsAdmin_Returns201() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");
    long countBefore = promoCodeRepository.count();

    mockMvc.perform(post("/api/v1/promo_codes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("SUPER_SALE", "FIXED", 500.0, true))
        .cookie(adminCookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.code").value("SUPER_SALE"))
      .andExpect(jsonPath("$.discount_type").value("FIXED"))
      .andExpect(jsonPath("$.value").value(500.0))
      .andExpect(jsonPath("$.usage_limit").value(100))
      .andExpect(jsonPath("$.is_active").value(true));

    assertEquals(countBefore + 1, promoCodeRepository.count());
    assertTrue(promoCodeRepository.existsByCode("SUPER_SALE"));
  }

  @Test
  void createPromoCode_DuplicateCode_Returns409() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(post("/api/v1/promo_codes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("DUPLICATE_CODE", "PERCENTAGE", 10.0, true))
        .cookie(adminCookie))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/promo_codes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("DUPLICATE_CODE", "FIXED", 100.0, true))
        .cookie(adminCookie))
      .andExpect(status().isConflict());
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/promo_codes/{id}
  // ─────────────────────────────────────────────

  @Test
  void updatePromoCode_AsAdmin_Returns200() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    PromoCode promo = new PromoCode();
    promo.setCode("OLD_CODE");
    promo.setDiscountType("PERCENTAGE");
    promo.setValue(BigDecimal.valueOf(10.0));
    promo.setIsActive(true);
    promo.setStartDate(OffsetDateTime.now());
    promo.setEndDate(OffsetDateTime.now().plusMonths(1));
    promo.setUsageLimit(50); // Додали ліміт для збереження
    promoCodeRepository.save(promo);

    mockMvc.perform(put("/api/v1/promo_codes/" + promo.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("NEW_CODE_UPDATED", "FIXED", 250.0, false))
        .cookie(adminCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("NEW_CODE_UPDATED"))
      .andExpect(jsonPath("$.value").value(250.0))
      .andExpect(jsonPath("$.is_active").value(false));
  }

  @Test
  void updatePromoCode_NonExisting_Returns404() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(put("/api/v1/promo_codes/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createPromoCodeJson("TEST_CODE", "PERCENTAGE", 10.0, true))
        .cookie(adminCookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/promo_codes (та інші GET методи)
  // ─────────────────────────────────────────────

  @Test
  void getAllPromoCodes_ReturnsList() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    PromoCode promo = new PromoCode();
    promo.setCode("GET_ALL_TEST");
    promo.setDiscountType("FIXED");
    promo.setValue(BigDecimal.valueOf(50));
    promo.setStartDate(OffsetDateTime.now());
    promo.setEndDate(OffsetDateTime.now().plusMonths(1));
    promo.setUsageLimit(10);
    promoCodeRepository.save(promo);

    mockMvc.perform(get("/api/v1/promo_codes")
        .cookie(adminCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
  }

  @Test
  void getPromoCodeByCode_Existing_ReturnsPromoCode() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    PromoCode promo = new PromoCode();
    promo.setCode("FIND_ME");
    promo.setDiscountType("PERCENTAGE");
    promo.setValue(BigDecimal.valueOf(15));
    promo.setStartDate(OffsetDateTime.now());
    promo.setEndDate(OffsetDateTime.now().plusMonths(1));
    promo.setUsageLimit(5);
    promoCodeRepository.save(promo);

    mockMvc.perform(get("/api/v1/promo_codes/by-code/FIND_ME")
        .cookie(adminCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("FIND_ME"))
      .andExpect(jsonPath("$.value").value(15.0));
  }

  @Test
  void getPromoCodeByCode_NonExisting_Returns404() throws Exception {
    Cookie adminCookie = loginAndGetCookie("admin@sellora.ua", "12345678");

    mockMvc.perform(get("/api/v1/promo_codes/by-code/INVALID_CODE_XYZ")
        .cookie(adminCookie))
      .andExpect(status().isNotFound());
  }
}
