package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
  // Рахуємо кількість поточних учасників у конкретній сесії
  int countBySessionId(Long sessionId);
  boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
}
