package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "store_shipping_methods")
@Data
public class StoreShippingMethod {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carrier_id", nullable = false)
  private ShippingCarrier carrier;

  @Column(name = "is_enabled")
  private Boolean isEnabled = true;
}
