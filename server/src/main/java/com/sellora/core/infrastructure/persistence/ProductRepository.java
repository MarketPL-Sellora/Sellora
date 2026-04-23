package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query("SELECT p FROM Product p JOIN Store s ON p.merchantId = s.id WHERE s.status = 'ACTIVE'")
  Page<Product> findAllActiveProducts(Pageable pageable);
}
