package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shipping_carriers")
@Data
public class ShippingCarrier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50, unique = true)
  private String code;

  @Column(name = "is_active")
  private Boolean isActive = true;
}
