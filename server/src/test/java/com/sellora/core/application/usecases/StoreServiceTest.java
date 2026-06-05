package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.MerchantRequisiteRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.exceptions.ConflictException;
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
  private MerchantRequisiteRepository merchantRequisiteRepository;
  @Mock
  private MerchantRequisiteService merchantRequisiteService;

  @InjectMocks
  private StoreService storeService;

  @Test
  void createStore_UserAlreadyHasStore_ThrowsConflictException() {
    Long userId = 1L;

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

    assertThrows(ConflictException.class, () -> storeService.createStore(request, userId));
    verify(storeRepository, never()).save(any());
  }

  @Test
  void createStore_ValidRequest_UpdatesRoleToMerchant() {
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

    storeService.createStore(request, userId);

    assertEquals("MERCHANT", user.getRole());
    verify(userRepository).save(user);
    verify(storeRepository).save(any(Store.class));
  }

  @Test
  void updateStoreStatus_AdminBlocksStore_ArchivesProductsAndCancelsSessions() {
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

    when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

    storeService.updateStoreStatus(storeId, "BLOCKED", adminId);

    assertEquals("BLOCKED", store.getStatus());
    verify(productRepository).archiveAllProductsByMerchantId(storeId);
    verify(groupBuySessionRepository).cancelAllActiveSessionsForMerchant(storeId);
    verify(storeRepository).save(any(Store.class));
  }

  @Test
  void updateStoreStatus_OwnerClosesWithActiveSessions_ThrowsConflictException() {
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
    when(groupBuySessionRepository.countActiveSessionsForMerchant(storeId)).thenReturn(5L);

    assertThrows(ConflictException.class, () -> storeService.updateStoreStatus(storeId, "CLOSED", ownerId));
    verify(storeRepository, never()).save(any());
  }

  @Test
  void deleteStore_WithExistingProducts_ThrowsConflictException() {
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
    when(productRepository.existsByMerchantId(storeId)).thenReturn(true);

    assertThrows(ConflictException.class, () -> storeService.deleteStore(storeId, ownerId));
    verify(storeRepository, never()).delete(any());
  }

  @Test
  void deleteStore_ValidRequest_RevertsRoleToBuyer() {
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

    storeService.deleteStore(storeId, ownerId);

    assertEquals("BUYER", owner.getRole());
    verify(userRepository).save(owner);
    verify(merchantRequisiteRepository).deleteAllByOwnerId(ownerId);
    verify(storeRepository).delete(store);
  }
}
