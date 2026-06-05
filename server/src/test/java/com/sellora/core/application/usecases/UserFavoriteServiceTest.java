package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.UserFavorite;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.UserFavoriteRepository;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import com.sellora.core.presentation.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserFavoriteServiceTest {

  @Mock
  private UserFavoriteRepository userFavoriteRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private UserFavoriteService userFavoriteService;

  private final Long userId = 1L;
  private final Long productId = 100L;

  @BeforeEach
  void setUp() {
    // Налаштовуємо SecurityContext за замовчуванням (авторизований користувач)
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);

    // Використовуємо lenient(), щоб уникнути помилок UnnecessaryStubbingException у тестах без авторизації
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.isAuthenticated()).thenReturn(true);
    lenient().when(authentication.getPrincipal()).thenReturn(userId);

    SecurityContextHolder.setContext(securityContext);
  }

  // --- ТЕСТИ ДОДАВАННЯ (addFavorite) ---

  @Test
  void addFavorite_Success_SavesToRepository() {
    when(productRepository.existsById(productId)).thenReturn(true);
    when(userFavoriteRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

    userFavoriteService.addFavorite(productId);

    verify(userFavoriteRepository, times(1)).save(any(UserFavorite.class));
  }

  @Test
  void addFavorite_AlreadyExists_DoesNotSaveAgain() {
    when(productRepository.existsById(productId)).thenReturn(true);
    when(userFavoriteRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

    userFavoriteService.addFavorite(productId);

    // Перевіряємо, що збереження не викликалось (ідемпотентність)
    verify(userFavoriteRepository, never()).save(any(UserFavorite.class));
  }

  @Test
  void addFavorite_ProductNotFound_ThrowsException() {
    when(productRepository.existsById(productId)).thenReturn(false);

    assertThrows(ResourceNotFoundException.class, () -> userFavoriteService.addFavorite(productId));
    verify(userFavoriteRepository, never()).save(any());
  }

  @Test
  void addFavorite_NotAuthenticated_ThrowsUnauthorizedException() {
    // Створюємо повністю новий контекст безпеки для цього тесту
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication auth = mock(Authentication.class);

    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn("anonymousUser");

    // Встановлюємо новий контекст, який перезапише той, що був у setUp()
    SecurityContextHolder.setContext(securityContext);

    assertThrows(UnauthorizedException.class, () -> userFavoriteService.addFavorite(productId));
    verify(userFavoriteRepository, never()).save(any());
  }

  // --- ТЕСТИ ВИДАЛЕННЯ (removeFavorite) ---

  @Test
  void removeFavorite_Success_DeletesFromRepository() {
    when(productRepository.existsById(productId)).thenReturn(true);
    when(userFavoriteRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

    userFavoriteService.removeFavorite(productId);

    verify(userFavoriteRepository, times(1)).deleteByUserIdAndProductId(userId, productId);
  }

  @Test
  void removeFavorite_NotInFavorites_DoesNotDelete() {
    when(productRepository.existsById(productId)).thenReturn(true);
    when(userFavoriteRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

    userFavoriteService.removeFavorite(productId);

    // Якщо товару не було в улюблених, delete викликатись не повинен (ідемпотентність)
    verify(userFavoriteRepository, never()).deleteByUserIdAndProductId(any(), any());
  }

  @Test
  void removeFavorite_ProductNotFound_ThrowsException() {
    when(productRepository.existsById(productId)).thenReturn(false);

    assertThrows(ResourceNotFoundException.class, () -> userFavoriteService.removeFavorite(productId));
    verify(userFavoriteRepository, never()).deleteByUserIdAndProductId(any(), any());
  }
}
