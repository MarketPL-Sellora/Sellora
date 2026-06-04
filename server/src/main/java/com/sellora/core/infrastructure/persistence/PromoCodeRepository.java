package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
  boolean existsByCode(String code);
  boolean existsByCodeAndIdNot(String code, Long id);
  Optional<PromoCode> findByCode(String code);
}
