package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.PromoUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoUsageHistoryRepository extends JpaRepository<PromoUsageHistory, Long> {
  boolean existsByUserIdAndPromoId(Long userId, Long promoId);
}
