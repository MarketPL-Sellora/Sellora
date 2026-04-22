package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stores")
@Data
public class Store {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "owner_id", nullable = false, unique = true)
  private Long ownerId;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, unique = true)
  private String slug;

  private String address;

  @Column(name = "contact_phone")
  private String contactPhone;

  private String description;

  @Column(name = "logo_url")
  private String logoUrl;

  private BigDecimal rating = BigDecimal.ZERO;

  @Column(nullable = false)
  private String status = "PENDING"; // Відповідає твоєму chk_stores_status

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
