package com.sellora.core.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "purchase_type")
  private String purchaseType;

  @Column(name = "session_id")
  private Long sessionId;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "store_id")
  private Long merchantId;

  @Column(name = "total_amount")
  private BigDecimal finalPrice;

  @Column(name = "payment_status")
  private String paymentStatus;

  @Column(name = "shipping_status")
  private String shippingStatus;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
