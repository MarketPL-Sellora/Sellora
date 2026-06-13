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

  @Query("SELECT s FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId ORDER BY s.createdAt DESC")
  List<GroupBuySession> findAllByUserId(@Param("userId") Long userId);

  @Query("SELECT s FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId AND s.status = :status ORDER BY s.createdAt DESC")
  List<GroupBuySession> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

  boolean existsByProductIdAndStatus(Long productId, String status);

  @Query("SELECT COUNT(s) FROM GroupBuySession s, Product p WHERE s.productId = p.id AND p.merchantId = :merchantId AND s.status = 'ACTIVE'")
  long countActiveSessionsForMerchant(@Param("merchantId") Long merchantId);

  @org.springframework.data.jpa.repository.Modifying
  @Query("UPDATE GroupBuySession s SET s.status = 'CANCELED' WHERE s.status = 'ACTIVE' AND s.productId IN (SELECT p.id FROM Product p WHERE p.merchantId = :merchantId)")
  void cancelAllActiveSessionsForMerchant(@Param("merchantId") Long merchantId);

  @Query("SELECT s.uuid FROM GroupBuySession s JOIN GroupMember m ON s.id = m.sessionId WHERE m.userId = :userId AND s.productId = :productId AND s.status = 'ACTIVE'")
  Optional<String> findActiveSessionUuidForUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

  // --- ФІКС: Перевірка тільки АКТИВНОЇ та НЕ ПРОСТРОЧЕНОЇ сесії ---
  @Query("SELECT COUNT(s) > 0 FROM GroupBuySession s LEFT JOIN GroupMember m ON s.id = m.sessionId WHERE (s.initiatorId = :userId OR m.userId = :userId) AND s.productId = :productId AND s.status = 'ACTIVE' AND s.expiresAt > :now")
  boolean hasActiveAndNotExpiredSessionForUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId, @Param("now") LocalDateTime now);
}
