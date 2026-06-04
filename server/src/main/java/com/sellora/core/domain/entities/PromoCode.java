package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "promo_codes")
@Data
public class PromoCode {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(name = "discount_type", nullable = false, length = 50)
  private String discountType; // "PERCENTAGE" або "FIXED"

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal value;

  @Column(name = "start_date")
  private OffsetDateTime startDate;

  @Column(name = "end_date")
  private OffsetDateTime endDate;

  @Column(name = "usage_limit")
  private Integer usageLimit;

  @Column(name = "used_count")
  private Integer usedCount = 0;

  @Column(name = "is_active")
  private Boolean isActive = true;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;
}
