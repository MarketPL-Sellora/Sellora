package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreResponseDto;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServiceTest {

  @Mock
  private StoreRepository storeRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private GroupBuySessionRepository groupBuySessionRepository;
  @Mock
  private ImageUploadService imageUploadService;

  @InjectMocks
  private StoreService storeService;

  // --- ТЕСТИ СТВОРЕННЯ МАГАЗИНУ ---

  @Test
  void createStore_UserAlreadyHasStore_ThrowsConflictException() {
    // Arrange
    Long userId = 1L;

    // Ініціалізація через сетери, щоб уникнути помилки відсутності конструктора
    CreateStoreRequest request = new CreateStoreRequest();
    request.setName("Shop Name");
    request.setAddress("Address");
    request.setContactPhone("123");
    request.setDescription("Desc");
    request.setLogoUrl("logo.jpg");

    User user = new User();
    user.setId(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(storeRepository.existsByOwnerId(userId)).thenReturn(true);

    // Act & Assert
    assertThrows(ConflictException.class, () -> storeService.createStore(request, userId));
    verify(storeRepository, never()).save(any());
  }

  @Test
  void createStore_ValidRequest_UpdatesRoleToMerchant() {
    // Arrange
    Long userId = 1L;

    CreateStoreRequest request = new CreateStoreRequest();
    request.setName("Shop Name");
    request.setAddress("Address");
    request.setContactPhone("123");
    request.setDescription("Desc");
    request.setLogoUrl("logo.jpg");

    User user = new User();
    user.setId(userId);
    user.setRole("BUYER");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(storeRepository.existsByOwnerId(userId)).thenReturn(false);

    // Act
    storeService.createStore(request, userId);

    // Assert
    assertEquals("MERCHANT", user.getRole());
    verify(userRepository).save(user);
    verify(storeRepository).save(any(Store.class));
  }
  // --- ТЕСТИ ОНОВЛЕННЯ СТАТУСУ ---

  @Test
  void updateStoreStatus_AdminBlocksStore_ArchivesProductsAndCancelsSessions() {
    // Arrange
    Long storeId = 10L;
    Long adminId = 1L;
    Store store = new Store();
    store.setId(storeId);
    store.setOwnerId(2L);
    store.setStatus("ACTIVE");

    User admin = new User();
    admin.setId(adminId);
    admin.setRole("ADMIN");

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

    // ДОДАНО: Мокаємо збереження, щоб метод повертав той самий об'єкт store
    when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    storeService.updateStoreStatus(storeId, "BLOCKED", adminId);

    // Assert
    assertEquals("BLOCKED", store.getStatus());
    verify(productRepository).archiveAllProductsByMerchantId(storeId);
    verify(groupBuySessionRepository).cancelAllActiveSessionsForMerchant(storeId);
    verify(storeRepository).save(any(Store.class));
  }

  @Test
  void updateStoreStatus_OwnerClosesWithActiveSessions_ThrowsConflictException() {
    // Arrange
    Long storeId = 10L;
    Long ownerId = 2L;
    Store store = new Store();
    store.setId(storeId);
    store.setOwnerId(ownerId);
    store.setStatus("ACTIVE");

    User owner = new User();
    owner.setId(ownerId);
    owner.setRole("MERCHANT");

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    // Імітуємо наявність активних сесій
    when(groupBuySessionRepository.countActiveSessionsForMerchant(storeId)).thenReturn(5L);

    // Act & Assert
    assertThrows(ConflictException.class, () -> storeService.updateStoreStatus(storeId, "CLOSED", ownerId));
    verify(storeRepository, never()).save(any());
  }

  // --- ТЕСТИ ВИДАЛЕННЯ МАГАЗИНУ ---

  @Test
  void deleteStore_WithExistingProducts_ThrowsConflictException() {
    // Arrange
    Long storeId = 10L;
    Long ownerId = 2L;
    Store store = new Store();
    store.setId(storeId);
    store.setOwnerId(ownerId);

    User owner = new User();
    owner.setId(ownerId);
    owner.setRole("MERCHANT");

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    // Імітуємо наявність товарів у магазині
    when(productRepository.existsByMerchantId(storeId)).thenReturn(true);

    // Act & Assert
    assertThrows(ConflictException.class, () -> storeService.deleteStore(storeId, ownerId));
    verify(storeRepository, never()).delete(any());
  }

  @Test
  void deleteStore_ValidRequest_RevertsRoleToBuyer() {
    // Arrange
    Long storeId = 10L;
    Long ownerId = 2L;
    Store store = new Store();
    store.setId(storeId);
    store.setOwnerId(ownerId);

    User owner = new User();
    owner.setId(ownerId);
    owner.setRole("MERCHANT");

    when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(productRepository.existsByMerchantId(storeId)).thenReturn(false);

    // Act
    storeService.deleteStore(storeId, ownerId);

    // Assert
    assertEquals("BUYER", owner.getRole());
    verify(userRepository).save(owner);
    verify(storeRepository).delete(store);
  }
}
