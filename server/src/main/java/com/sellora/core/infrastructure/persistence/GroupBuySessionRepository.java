package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.GroupBuySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBuySessionRepository extends JpaRepository<GroupBuySession, Long> {
  Optional<GroupBuySession> findByUuid(String uuid);

  List<GroupBuySession> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);

  //Всі сесії користувача (незалежно від статусу)
  @Query("SELECT s FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId ORDER BY s.createdAt DESC")
  List<GroupBuySession> findAllByUserId(@Param("userId") Long userId);

  // Виправлено: звертаємось до полів
  @Query("SELECT s FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId AND s.status = :status ORDER BY s.createdAt DESC")
  List<GroupBuySession> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
  boolean existsByProductIdAndStatus(Long productId, String status);
// (Якщо статус у тебе Enum, то зміни String на назву твого Enum, наприклад SessionStatus)

  // Перевіряємо кількість активних сесій продавця (безпечний JPQL запит без явного JOIN)
  @Query("SELECT COUNT(s) FROM GroupBuySession s, Product p WHERE s.productId = p.id AND p.merchantId = :merchantId AND s.status = 'ACTIVE'")
  long countActiveSessionsForMerchant(@org.springframework.data.repository.query.Param("merchantId") Long merchantId);

  // Масово скасовуємо всі активні сесії для всіх товарів конкретного продавця
  @org.springframework.data.jpa.repository.Modifying
  @Query("UPDATE GroupBuySession s SET s.status = 'CANCELED' WHERE s.status = 'ACTIVE' AND s.productId IN (SELECT p.id FROM Product p WHERE p.merchantId = :merchantId)")
  void cancelAllActiveSessionsForMerchant(@org.springframework.data.repository.query.Param("merchantId") Long merchantId);

  @Query("SELECT s.uuid FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId AND s.productId = :productId AND s.status = 'ACTIVE'")
  java.util.Optional<String> findActiveSessionUuidForUserAndProduct(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("productId") Long productId);

}
