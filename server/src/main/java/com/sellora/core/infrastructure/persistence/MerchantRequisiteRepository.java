package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.MerchantRequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantRequisiteRepository extends JpaRepository<MerchantRequisite, Long> {
  List<MerchantRequisite> findByOwnerId(Long ownerId);
  Optional<MerchantRequisite> findByOwnerIdAndIsPrimaryTrue(Long ownerId);
  long countByOwnerId(Long ownerId);
  void deleteAllByOwnerId(Long ownerId);
}
