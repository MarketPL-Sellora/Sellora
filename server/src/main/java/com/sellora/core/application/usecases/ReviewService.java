package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.ProductReview;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.OrderRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.ProductReviewRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.ReviewRequestDto;
import com.sellora.core.presentation.dtos.ReviewResponseDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ProductReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;

  @Transactional
  public ReviewResponseDto createReview(Long productId, Long userId, ReviewRequestDto dto) {
    if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("Користувача не знайдено");

    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
      throw new ConflictException("Ви вже залишали відгук на цей товар");
    }

    boolean hasBought = orderRepository.hasUserBoughtAndReceivedProduct(userId, productId);
    if (!hasBought) {
      throw new ForbiddenException("Ви можете залишити відгук лише після отримання товару");
    }

    ProductReview review = new ProductReview();
    review.setProductId(productId);
    review.setUserId(userId);
    review.setRating(dto.rating());
    review.setComment(dto.comment());

    ProductReview savedReview = reviewRepository.save(review);
    updateProductRatingStats(product);

    return mapToDto(savedReview);
  }

  @Transactional
  public void deleteReview(Long productId, Long requesterId) {
    if (!userRepository.existsById(requesterId)) throw new ResourceNotFoundException("Користувача не знайдено");

    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    ProductReview review = reviewRepository.findByUserIdAndProductId(requesterId, productId)
      .orElseThrow(() -> new ResourceNotFoundException("Відгук не знайдено"));

    User requester = userRepository.findById(requesterId).get();

    if (!review.getUserId().equals(requesterId) && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ForbiddenException("Тільки автор або адміністратор може видалити відгук");
    }

    reviewRepository.delete(review);
    updateProductRatingStats(product);
  }

  private void updateProductRatingStats(Product product) {
    long count = reviewRepository.countByProductId(product.getId());
    Double avgRating = reviewRepository.calculateAverageRating(product.getId());

    product.setReviewsCount((int) count);
    if (avgRating != null) {
      product.setRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
    } else {
      product.setRating(BigDecimal.ZERO);
    }

    productRepository.save(product);
  }

  private ReviewResponseDto mapToDto(ProductReview review) {
    return new ReviewResponseDto(
      review.getId(),
      review.getProductId(),
      review.getUserId(),
      review.getRating(),
      review.getComment(),
      review.getCreatedAt(),
      review.getUpdatedAt()
    );
  }

  // Додай ці методи у свій ReviewService:

  @Transactional
  public ReviewResponseDto updateReview(Long productId, Long userId, ReviewRequestDto dto) {
    ProductReview review = reviewRepository.findByUserIdAndProductId(userId, productId)
      .orElseThrow(() -> new ResourceNotFoundException("Відгук не знайдено"));

    review.setRating(dto.rating());
    review.setComment(dto.comment());

    ProductReview savedReview = reviewRepository.save(review);

    // Перераховуємо середній рейтинг (reviews_count залишиться тим самим)
    Product product = productRepository.findById(productId).get();
    updateProductRatingStats(product);

    return mapToDto(savedReview);
  }

  @Transactional(readOnly = true)
  public com.sellora.core.presentation.dtos.ReviewCheckResponseDto checkEligibility(Long productId, Long userId) {
    if (userId == null) {
      return new com.sellora.core.presentation.dtos.ReviewCheckResponseDto(false, "NOT_AUTHENTICATED");
    }

    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Товар не знайдено");
    }

    if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
      return new com.sellora.core.presentation.dtos.ReviewCheckResponseDto(false, "ALREADY_REVIEWED");
    }

    boolean hasBought = orderRepository.hasUserBoughtAndReceivedProduct(userId, productId);
    if (!hasBought) {
      return new com.sellora.core.presentation.dtos.ReviewCheckResponseDto(false, "NOT_PURCHASED");
      // Або NOT_DELIVERED, в залежності від того, чи хочете розділяти логіку на фронті
    }

    return new com.sellora.core.presentation.dtos.ReviewCheckResponseDto(true, null);
  }
}
