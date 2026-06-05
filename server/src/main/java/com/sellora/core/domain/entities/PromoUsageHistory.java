package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "promo_usage_history")
@Data
public class PromoUsageHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "promo_id", nullable = false)
  private Long promoId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "used_at", insertable = false, updatable = false)
  private OffsetDateTime usedAt;
}
