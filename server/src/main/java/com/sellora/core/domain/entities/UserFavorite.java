package com.sellora.core.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
class UserFavoriteId implements Serializable {
  private Long userId;
  private Long productId;
}

@Entity
@Table(name = "user_favorites")
@Data
@IdClass(UserFavoriteId.class)
public class UserFavorite {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(name = "product_id")
  private Long productId;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}
