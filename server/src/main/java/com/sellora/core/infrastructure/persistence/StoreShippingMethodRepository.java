package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.StoreShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreShippingMethodRepository extends JpaRepository<StoreShippingMethod, Long> {
  List<StoreShippingMethod> findByStoreId(Long storeId);
}
