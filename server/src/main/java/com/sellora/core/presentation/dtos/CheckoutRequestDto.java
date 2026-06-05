package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CheckoutRequestDto {
  @NotNull @JsonProperty("store_id") private Long storeId;

  @NotBlank @JsonProperty("buyer_name") private String buyerName;
  @NotBlank @JsonProperty("buyer_surname") private String buyerSurname;
  @NotBlank @JsonProperty("buyer_phone") private String buyerPhone;
  @NotBlank @JsonProperty("buyer_email") private String buyerEmail;

  @NotBlank @JsonProperty("delivery_type") private String deliveryType; // BRANCH, COURIER, PICKUP
  @JsonProperty("carrier_id") private Long carrierId;
  @JsonProperty("delivery_address") private Map<String, Object> deliveryAddress;

  @NotBlank @JsonProperty("payment_method") private String paymentMethod; // ONLINE_CARD, CASH_ON_DELIVERY
  @JsonProperty("order_comment") private String orderComment;

  @JsonProperty("promo_code") private String promoCode;

  @NotEmpty @JsonProperty("items") private List<CheckoutItemDto> items;
}
