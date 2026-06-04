package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Long> {
  boolean existsByCode(String code);
  boolean existsByCodeAndIdNot(String code, Long id);
}
