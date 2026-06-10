package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

  // Корисно для перевірки, чи LiqPay не надіслав нам той самий статус двічі
  boolean existsByIdempotencyKey(String idempotencyKey);

  List<TransactionEvent> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
