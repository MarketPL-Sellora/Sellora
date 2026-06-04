package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transaction_events")
@Data
public class TransactionEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "event_type", nullable = false)
  private String eventType; // success, error, refund

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;
}
