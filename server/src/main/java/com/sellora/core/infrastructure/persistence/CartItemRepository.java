package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

  List<CartItem> findByCartId(Long cartId);

  // --- НОВІ МЕТОДИ ДЛЯ ВИДАЛЕННЯ ---
  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
  void deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
  void deleteAllByCartId(@Param("cartId") Long cartId);
}
