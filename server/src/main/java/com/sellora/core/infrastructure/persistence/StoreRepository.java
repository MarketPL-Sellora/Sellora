package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

  Optional<Store> findByOwnerId(Long ownerId);

  boolean existsByOwnerId(Long ownerId);
  List<Store> findAllByStatus(String status);
  Optional<Store> findByIdAndStatus(Long id, String status);
}
