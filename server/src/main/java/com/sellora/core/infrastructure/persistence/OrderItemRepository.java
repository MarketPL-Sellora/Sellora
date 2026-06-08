package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {



  // Отримання всіх товарів для списку замовлень (оптимізація запитів)
  List<OrderItem> findByOrderIdIn(List<Long> orderIds);

  List<com.sellora.core.domain.entities.OrderItem> findByOrderId(Long orderId);
}
