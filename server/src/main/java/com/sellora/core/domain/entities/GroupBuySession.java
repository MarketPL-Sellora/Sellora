package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_buy_sessions")
@Data
public class GroupBuySession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // UUID для безпечного шарінгу посилання (щоб не світити ID з бази)
  @Column(name = "uuid", unique = true, nullable = false, updatable = false)
  private String uuid;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "locked_price")
  private java.math.BigDecimal lockedPrice;

  @Column(name = "locked_target_size")
  private Integer lockedTargetSize;

  @Column(name = "initiator_id")
  private Long initiatorId;

  private String status; // 'ACTIVE', 'COMPLETED', 'CANCELLED'

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
