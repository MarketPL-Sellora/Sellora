package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.GroupBuySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBuySessionRepository extends JpaRepository<GroupBuySession, Long> {
  Optional<GroupBuySession> findByUuid(String uuid);
  List<GroupBuySession> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);
}
