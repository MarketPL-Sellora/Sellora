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
}
