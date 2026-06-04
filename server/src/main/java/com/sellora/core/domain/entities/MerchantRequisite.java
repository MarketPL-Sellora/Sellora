package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "merchant_requisites")
@Data
public class MerchantRequisite {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  @Column(length = 10, nullable = false)
  private String edrpou;

  @Column(length = 29, nullable = false)
  private String iban;

  @Column(name = "bank_name", nullable = false)
  private String bankName;

  @Column(name = "is_primary")
  private Boolean isPrimary = false;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;
}
