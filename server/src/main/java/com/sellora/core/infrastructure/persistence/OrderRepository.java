package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Order;
import com.sellora.core.domain.entities.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query("SELECT o FROM Order o WHERE o.userId = :userId " +
    "AND (:shippingStatus IS NULL OR o.shippingStatus = :shippingStatus) " +
    "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)")
  Page<Order> findUserOrders(
    @Param("userId") Long userId,
    @Param("shippingStatus") String shippingStatus,
    @Param("paymentStatus") String paymentStatus,
    Pageable pageable
  );



}
