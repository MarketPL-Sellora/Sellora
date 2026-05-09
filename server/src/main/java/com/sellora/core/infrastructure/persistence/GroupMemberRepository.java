package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
  // Рахуємо кількість поточних учасників у конкретній сесії
  int countBySessionId(Long sessionId);

  boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

  // Отримання всіх учасників сесії
  List<GroupMember> findBySessionId(Long sessionId);

  // Перевірка, чи юзер є ініціатором/учасником АКТИВНОЇ сесії на конкретний товар
  // НОВЕ: Перевірка (Task 3), чи юзер є ініціатором/учасником АКТИВНОЇ сесії на конкретний товар
  @Query("SELECT COUNT(m) > 0 FROM GroupMember m JOIN GroupBuySession s ON m.sessionId = s.id WHERE m.userId = :userId AND s.productId = :productId AND s.status = 'ACTIVE'")
  boolean isUserInActiveSessionForProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
