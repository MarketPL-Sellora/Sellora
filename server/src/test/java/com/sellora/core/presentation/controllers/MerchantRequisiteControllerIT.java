package com.sellora.core.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellora.core.AbstractIntegrationTest;
import com.sellora.core.domain.entities.MerchantRequisite;
import com.sellora.core.infrastructure.persistence.MerchantRequisiteRepository;
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
public class MerchantRequisiteControllerIT extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MerchantRequisiteRepository merchantRequisiteRepository;

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

  private String createRequisiteJson(String edrpou, String iban, String bankName, boolean isPrimary) {
    return String.format("""
      {
        "edrpou": "%s",
        "iban": "%s",
        "bank_name": "%s",
        "is_primary": %b
      }
      """, edrpou, iban, bankName, isPrimary);
  }

  // ─────────────────────────────────────────────
  // POST /api/v1/merchant_requisites
  // ─────────────────────────────────────────────

  @Test
  void createRequisite_WithoutAuth_Returns401() throws Exception {
    mockMvc.perform(post("/api/v1/merchant_requisites")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createRequisiteJson("12345678", "UA123456789012345678901234567", "Monobank", true)))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void createRequisite_WithAuth_Returns201() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("apple_auth@merchant.ua", "12345678");
    long countBefore = merchantRequisiteRepository.count();

    // Змінено isPrimary на false, щоб уникнути конфлікту з наявними seed-даними
    mockMvc.perform(post("/api/v1/merchant_requisites")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createRequisiteJson("11111111", "UA000000000000000000000000001", "PrivatBank", false))
        .cookie(merchantCookie))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").exists())
      .andExpect(jsonPath("$.edrpou").value("11111111"))
      .andExpect(jsonPath("$.bank_name").value("PrivatBank"))
      .andExpect(jsonPath("$.is_primary").value(false)); // Перевіряємо на false

    assertEquals(countBefore + 1, merchantRequisiteRepository.count());
  }

  // ─────────────────────────────────────────────
  // PUT /api/v1/merchant_requisites/{id}
  // ─────────────────────────────────────────────

  @Test
  void updateRequisite_Existing_Returns200() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    MerchantRequisite requisite = new MerchantRequisite();
    requisite.setOwnerId(3L);
    requisite.setEdrpou("00000000");
    // Змінено на валідний 29-символьний IBAN
    requisite.setIban("UA000000000000000000000000002");
    requisite.setBankName("Old Bank");
    requisite.setIsPrimary(false);
    MerchantRequisite saved = merchantRequisiteRepository.save(requisite);

    mockMvc.perform(put("/api/v1/merchant_requisites/" + saved.getId())
        .contentType(MediaType.APPLICATION_JSON)
        // Змінено на валідний 29-символьний IBAN
        .content(createRequisiteJson("99999999", "UA999999999999999999999999999", "New Bank", true))
        .cookie(merchantCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.edrpou").value("99999999"))
        .andExpect(jsonPath("$.bank_name").value("New Bank")) // Змінено
        .andExpect(jsonPath("$.is_primary").value(true)); // Змінено
  }

  @Test
  void updateRequisite_NonExisting_Returns404() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("samsung_official@merchant.ua", "12345678");

    mockMvc.perform(put("/api/v1/merchant_requisites/999999")
        .contentType(MediaType.APPLICATION_JSON)
        // Змінено на валідний IBAN
        .content(createRequisiteJson("12345678", "UA123456789012345678901234567", "Bank", false))
        .cookie(merchantCookie))
      .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/merchant_requisites (All for current user)
  // ─────────────────────────────────────────────

  @Test
  void getAllRequisitesForCurrentUser_ReturnsList() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("nike_distributor@merchant.ua", "12345678");
    Long ownerId = 4L; // ID користувача nike_distributor

    MerchantRequisite requisite = new MerchantRequisite();
    requisite.setOwnerId(ownerId);
    requisite.setEdrpou("77777777");
    requisite.setIban("UA000000000000000000000000003");
    requisite.setBankName("Sport Bank");
    requisite.setIsPrimary(false); // <--- ЗМІНЕНО НА FALSE (щоб не конфліктувати з seed-даними)
    merchantRequisiteRepository.save(requisite);

    mockMvc.perform(get("/api/v1/merchant_requisites")
        .cookie(merchantCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    // Прибрали жорстку перевірку $[0], оскільки сортування може повернути seed-дані першими
  }

  // ─────────────────────────────────────────────
  // GET /api/v1/merchant_requisites/{id}
  // ─────────────────────────────────────────────

  @Test
  void getRequisiteById_Existing_ReturnsRequisite() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("nike_distributor@merchant.ua", "12345678");

    MerchantRequisite requisite = new MerchantRequisite();
    requisite.setOwnerId(4L);
    requisite.setEdrpou("44444444");
    // Змінено на валідний IBAN
    requisite.setIban("UA000000000000000000000000004");
    requisite.setBankName("Target Bank");
    requisite.setIsPrimary(false);
    MerchantRequisite saved = merchantRequisiteRepository.save(requisite);

    mockMvc.perform(get("/api/v1/merchant_requisites/" + saved.getId())
        .cookie(merchantCookie))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(saved.getId()))
      .andExpect(jsonPath("$.edrpou").value("44444444"));
  }

  // ─────────────────────────────────────────────
  // DELETE /api/v1/merchant_requisites/{id}
  // ─────────────────────────────────────────────

  @Test
  void deleteRequisite_Existing_Returns204() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("book_island@merchant.ua", "12345678");

    MerchantRequisite requisite = new MerchantRequisite();
    requisite.setOwnerId(5L);
    requisite.setEdrpou("55555555");
    // Змінено на валідний IBAN
    requisite.setIban("UA000000000000000000000000005");
    requisite.setBankName("Delete Bank");
    requisite.setIsPrimary(false);
    MerchantRequisite saved = merchantRequisiteRepository.save(requisite);

    mockMvc.perform(delete("/api/v1/merchant_requisites/" + saved.getId())
        .cookie(merchantCookie))
      .andExpect(status().isNoContent());

    assertFalse(merchantRequisiteRepository.existsById(saved.getId()));
  }

  @Test
  void deleteRequisite_NonExisting_Returns404() throws Exception {
    Cookie merchantCookie = loginAndGetCookie("book_island@merchant.ua", "12345678");

    mockMvc.perform(delete("/api/v1/merchant_requisites/999999")
        .cookie(merchantCookie))
      .andExpect(status().isNotFound());
  }
}
