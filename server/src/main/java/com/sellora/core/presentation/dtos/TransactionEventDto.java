package com.sellora.core.presentation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime; // <--- Змінено імпорт

public record TransactionEventDto(
  Long id,
  @JsonProperty("idempotency_key") String idempotencyKey,
  @JsonProperty("order_id") Long orderId,
  @JsonProperty("event_type") String eventType,
  BigDecimal amount,
  @JsonProperty("created_at") OffsetDateTime createdAt
) {}
