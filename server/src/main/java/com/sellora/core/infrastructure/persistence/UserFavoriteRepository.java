package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Object> {

  @Query("SELECT uf.productId FROM UserFavorite uf WHERE uf.userId = :userId AND uf.productId IN :productIds")
  Set<Long> findFavoriteProductIdsByUserAndProducts(
    @Param("userId") Long userId,
    @Param("productIds") List<Long> productIds
  );

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  // --- НОВИЙ МЕТОД ДЛЯ ВИДАЛЕННЯ ---
  @Modifying
  @Transactional
  @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId AND uf.productId = :productId")
  void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
