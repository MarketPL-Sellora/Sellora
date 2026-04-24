package com.sellora.core.domain.entities;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Data
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "store_id")
  private Long merchantId;

  @Column(name = "category_id")
  private Long categoryId;

  private String title; // Якщо ім'я колонки і змінної збігаються, @Column можна не писати

  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attributes", columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "images", columnDefinition = "jsonb")
  private List<String> images;

  @Column(name = "stock_quantity")
  private Integer stockQuantity;

  @Column(name = "standard_price")
  private BigDecimal standardPrice;

  @Column(name = "group_price")
  private BigDecimal groupPrice;

  @Column(name = "group_target_size")
  private Integer groupTargetSize;

  private String status;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
