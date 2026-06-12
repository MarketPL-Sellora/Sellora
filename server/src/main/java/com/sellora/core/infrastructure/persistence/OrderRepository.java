package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  // Для оновлення статусів після успішного збору групи (використаємо у PaymentService)
  List<Order> findBySessionId(Long sessionId);

  Page<Order> findAllByUserId(Long userId, Pageable pageable);

  @Query("SELECT o FROM Order o WHERE o.merchantId = :storeId " +
    "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
    "AND (:shippingStatus IS NULL OR o.shippingStatus = :shippingStatus)")
  Page<Order> findStoreOrdersWithFilters(
    @Param("storeId") Long storeId,
    @Param("paymentStatus") String paymentStatus,
    @Param("shippingStatus") String shippingStatus,
    Pageable pageable
  );


  @Query("SELECT o FROM Order o WHERE o.paymentMethod = 'ONLINE_CARD' " +
    "AND o.paymentStatus = 'PENDING' AND o.createdAt < :threshold")
  java.util.List<Order> findExpiredPendingOrders(@org.springframework.data.repository.query.Param("threshold") LocalDateTime threshold);


  @Query(value = "SELECT COUNT(o.id) > 0 FROM orders o " +
    "JOIN order_items oi ON o.id = oi.order_id " +
    "WHERE o.user_id = :userId " +
    "AND oi.product_id = :productId " +
    "AND o.payment_status = 'PAID' " +
    "AND o.shipping_status = 'DELIVERED'",
    nativeQuery = true)
  boolean hasUserBoughtAndReceivedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

}
