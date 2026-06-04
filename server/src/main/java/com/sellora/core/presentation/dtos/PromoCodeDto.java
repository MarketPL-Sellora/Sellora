package com.sellora.core.presentation.dtos;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromoCodeDto(
  Long id,
  @NotBlank String code,
  @NotBlank @JsonProperty("discount_type") String discountType,
  @NotNull BigDecimal value,
  @JsonProperty("start_date") OffsetDateTime startDate,
  @JsonProperty("end_date") OffsetDateTime endDate,
  @JsonProperty("usage_limit") Integer usageLimit,
  @JsonProperty("used_count") Integer usedCount,
  @NotNull @JsonProperty("is_active") Boolean isActive,
  @JsonProperty("created_at") OffsetDateTime createdAt
) {}
