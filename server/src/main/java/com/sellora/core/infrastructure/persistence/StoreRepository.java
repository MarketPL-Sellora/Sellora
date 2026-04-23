package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
  boolean existsByOwnerId(Long ownerId);
  // Пошук тільки тих, хто має статус ACTIVE
  List<Store> findAllByStatus(String status);
  // Пошук конкретного магазину, тільки якщо він не заблокований
  Optional<Store> findByIdAndStatus(Long id, String status);
}
