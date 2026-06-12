package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.ProductReview;
import com.sellora.core.presentation.dtos.ReviewWithUserDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

  boolean existsByUserIdAndProductId(Long userId, Long productId);

  Optional<ProductReview> findByUserIdAndProductId(Long userId, Long productId);

  long countByProductId(Long productId);

  @Query("SELECT AVG(CAST(r.rating AS double)) FROM ProductReview r WHERE r.productId = :productId")
  Double calculateAverageRating(@Param("productId") Long productId);

  @Query("SELECT new com.sellora.core.presentation.dtos.ReviewWithUserDto(" +
    "r.id, r.userId, u.email, r.rating, r.comment, r.createdAt) " +
    "FROM ProductReview r JOIN User u ON r.userId = u.id " +
    "WHERE r.productId = :productId ORDER BY r.createdAt DESC")
  List<ReviewWithUserDto> findReviewsWithUserEmailByProductId(@Param("productId") Long productId);
}
