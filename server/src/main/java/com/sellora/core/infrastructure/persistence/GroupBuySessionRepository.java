package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.GroupBuySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupBuySessionRepository extends JpaRepository<GroupBuySession, Long> {
  // Магічний метод Спрінга: знайде сесію за її UUID
  Optional<GroupBuySession> findByUuid(String uuid);
}
