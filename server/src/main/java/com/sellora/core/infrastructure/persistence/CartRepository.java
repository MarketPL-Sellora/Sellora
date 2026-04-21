package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
  // Шукаємо кошик за ID користувача
  Optional<Cart> findByUserId(Long userId);
}
