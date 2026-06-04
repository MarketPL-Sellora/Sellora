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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  // --- НОВІ ПОЛЯ ЗІ СКРІНШОТА ---

  @Column(name = "buyer_name", nullable = false)
  private String buyerName;

  @Column(name = "buyer_surname")
  private String buyerSurname;

  @Column(name = "buyer_phone")
  private String buyerPhone;

  @Column(name = "buyer_email")
  private String buyerEmail;

  @Column(name = "delivery_type")
  private String deliveryType;

  @Column(name = "carrier_id")
  private Long carrierId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "delivery_address", columnDefinition = "jsonb")
  private String deliveryAddress;

  @Column(name = "tracking_number")
  private String trackingNumber;

  @Column(name = "payment_method")
  private String paymentMethod;

  @Column(name = "order_comment", columnDefinition = "text")
  private String orderComment;

  @Column(name = "subtotal")
  private BigDecimal subtotal;

  @Column(name = "tax")
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(name = "discount")
  private BigDecimal discount = BigDecimal.ZERO;

  // ------------------------------

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
