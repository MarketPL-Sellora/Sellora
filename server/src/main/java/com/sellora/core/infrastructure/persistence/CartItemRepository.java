package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  // Перевіряємо, чи є вже такий товар у конкретному кошику
  Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
  List<CartItem> findByCartId(Long cartId);
}
