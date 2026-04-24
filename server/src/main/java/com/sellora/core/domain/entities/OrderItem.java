package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "quantity")
  private Integer quantity;

  @Column(name = "price_snapshot")
  private BigDecimal priceSnapshot;

  @Column(name = "title_snapshot")
  private String titleSnapshot;

  @Column(name = "image_snapshot")
  private String imageSnapshot;
}
