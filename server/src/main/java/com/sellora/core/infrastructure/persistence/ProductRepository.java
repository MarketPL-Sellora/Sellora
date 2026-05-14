package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  @Query("SELECT p FROM Product p JOIN Store s ON p.merchantId = s.id WHERE s.status = 'ACTIVE'")
  Page<Product> findAllActiveProducts(Pageable pageable);

  Page<Product> findAllByMerchantId(Long merchantId, Pageable pageable);

  // НОВЕ: Перевірка наявності товарів у категорії
  boolean existsByCategoryId(Long categoryId);

  // Додай цей метод у ProductRepository
  boolean existsByMerchantId(Long merchantId);

  @Modifying
  @Query("UPDATE Product p SET p.status = 'ARCHIVED' WHERE p.merchantId = :merchantId")
  void archiveAllProductsByMerchantId(@Param("merchantId") Long merchantId);
}
