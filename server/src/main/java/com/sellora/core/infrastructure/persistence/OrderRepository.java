package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  // Для оновлення статусів після успішного збору групи (використаємо у PaymentService)
  List<Order> findBySessionId(Long sessionId);
}
