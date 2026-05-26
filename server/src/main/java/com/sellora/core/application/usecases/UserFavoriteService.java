package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.UserFavorite;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.UserFavoriteRepository;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFavoriteService {

  private final UserFavoriteRepository userFavoriteRepository;
  private final ProductRepository productRepository;

  // Хелпер для отримання користувача з перевіркою (Пункт 1)
  private Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
      throw new UnauthorizedException("Потрібна авторизація для керування улюбленими товарами");
    }
    return (Long) auth.getPrincipal();
  }

  @Transactional
  public void addFavorite(Long productId) {
    Long userId = getCurrentUserId();

    // Перевірка, чи існує товар (Пункт 2)
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Товар з ID " + productId + " не знайдено");
    }

    // Якщо товару ще немає в улюблених - додаємо (Пункти 3 і 4)
    if (!userFavoriteRepository.existsByUserIdAndProductId(userId, productId)) {
      UserFavorite favorite = new UserFavorite();
      favorite.setUserId(userId);
      favorite.setProductId(productId);
      userFavoriteRepository.save(favorite);
    }
    // Якщо вже є, транзакція просто успішно завершиться (повернемо 201)
  }

  @Transactional
  public void removeFavorite(Long productId) {
    Long userId = getCurrentUserId();

    // Перевірка, чи існує товар (Пункт 2)
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Товар з ID " + productId + " не знайдено");
    }

    // Якщо товар є в улюблених - видаляємо (Пункти 3 і 4)
    if (userFavoriteRepository.existsByUserIdAndProductId(userId, productId)) {
      userFavoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }
    // Якщо немає, транзакція успішно завершиться (повернемо 204)
  }
}
