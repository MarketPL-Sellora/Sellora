package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.*;
import com.sellora.core.infrastructure.persistence.*;
import com.sellora.core.presentation.dtos.*;
import com.sellora.core.presentation.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreService {

  private final StoreRepository storeRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final GroupBuySessionRepository groupBuySessionRepository;
  private final ShippingCarrierRepository shippingCarrierRepository;

  // --- ДОДАНО НОВІ ІН'ЄКЦІЇ ---
  private final MerchantRequisiteService merchantRequisiteService;
  private final MerchantRequisiteRepository merchantRequisiteRepository;

  @Transactional
  public void createStore(CreateStoreRequest request, Long userId) {
    User currentUser = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    if (storeRepository.existsByOwnerId(currentUser.getId())) {
      throw new ConflictException("У вас вже є створений магазин!");
    }

    Store store = new Store();
    store.setOwnerId(currentUser.getId());
    store.setName(request.getName());
    store.setSlug(generateSlug(request.getName()));
    store.setAddress(request.getAddress());
    store.setContactPhone(request.getContactPhone());
    store.setDescription(request.getDescription());
    store.setLogoUrl(request.getLogoUrl());
    store.setStatus("PENDING");

    // --- ЛОГІКА ДОДАВАННЯ МЕТОДІВ ДОСТАВКИ (Атомарно) ---
    if (request.getShippingMethods() != null) {
      for (StoreShippingMethodDto methodDto : request.getShippingMethods()) {
        ShippingCarrier carrier = shippingCarrierRepository.findById(methodDto.carrierId())
          .orElseThrow(() -> new ResourceNotFoundException("Службу доставки з ID " + methodDto.carrierId() + " не знайдено"));

        StoreShippingMethod storeMethod = new StoreShippingMethod();
        storeMethod.setStore(store);
        storeMethod.setCarrier(carrier);
        storeMethod.setIsEnabled(methodDto.isEnabled());
        store.getShippingMethods().add(storeMethod);
      }
    }

    storeRepository.save(store);

    // --- ЛОГІКА ДОДАВАННЯ РЕКВІЗИТІВ (Атомарно) ---
    if (request.getMerchantRequisites() != null && !request.getMerchantRequisites().isEmpty()) {
      for (MerchantRequisiteDto reqDto : request.getMerchantRequisites()) {
        merchantRequisiteService.create(reqDto, currentUser.getId());
      }
    }

    if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
      currentUser.setRole("MERCHANT");
      userRepository.save(currentUser);
    }
  }

  @Transactional(readOnly = true)
  public StoreResponseDto getStoreByUserId(Long userId) {
    Store store = storeRepository.findByOwnerId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));
    return mapToDto(store);
  }

  @Transactional
  public StoreResponseDto updateStore(Long storeId, UpdateStoreRequest request, Long userId) {
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    if (!store.getOwnerId().equals(userId)) {
      throw new ForbiddenException("Ви не маєте прав редагувати цей магазин");
    }

    if (!store.getName().equalsIgnoreCase(request.name())) {
      if (storeRepository.existsByNameAndIdNot(request.name(), storeId)) throw new ConflictException("Магазин з такою назвою вже існує");
      String newSlug = generateSlug(request.name());
      if (storeRepository.existsBySlugAndIdNot(newSlug, storeId)) throw new ConflictException("Slug вже зайнятий");
      store.setName(request.name());
      store.setSlug(newSlug);
    }

    store.setAddress(request.address());
    store.setContactPhone(request.contactPhone());
    store.setDescription(request.description());
    store.setLogoUrl(request.logoUrl());

    // --- ОНОВЛЕННЯ МЕТОДІВ ДОСТАВКИ ---
    if (request.shippingMethods() != null) {
      store.getShippingMethods().clear();
      for (StoreShippingMethodDto methodDto : request.shippingMethods()) {
        ShippingCarrier carrier = shippingCarrierRepository.findById(methodDto.carrierId())
          .orElseThrow(() -> new ResourceNotFoundException("Служба доставки не знайдена"));
        StoreShippingMethod storeMethod = new StoreShippingMethod();
        storeMethod.setStore(store);
        storeMethod.setCarrier(carrier);
        storeMethod.setIsEnabled(methodDto.isEnabled());
        store.getShippingMethods().add(storeMethod);
      }
    }

    Store updatedStore = storeRepository.save(store);
    return mapToDto(updatedStore);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getStoreShippingMethods(Long storeId) {
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    List<StoreShippingMethodDto> methods = store.getShippingMethods().stream()
      .map(m -> new StoreShippingMethodDto(m.getCarrier().getId(), m.getIsEnabled()))
      .toList();

    return Map.of("store_id", storeId, "shipping_methods", methods);
  }

  @Transactional
  public StoreResponseDto updateStoreStatus(Long storeId, String newStatus, Long userId) {
    Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));
    User currentUser = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
    boolean isOwner = store.getOwnerId().equals(userId);

    if (!isAdmin && !isOwner) throw new ForbiddenException("Ви не маєте прав змінювати статус");

    if ("BLOCKED".equalsIgnoreCase(newStatus)) {
      if (!isAdmin) throw new ForbiddenException("Тільки адміністратор може заблокувати");
      productRepository.archiveAllProductsByMerchantId(storeId);
      groupBuySessionRepository.cancelAllActiveSessionsForMerchant(storeId);
    } else if ("CLOSED".equalsIgnoreCase(newStatus)) {
      if (!isOwner) throw new ForbiddenException("Тільки власник може закрити");
      if (groupBuySessionRepository.countActiveSessionsForMerchant(storeId) > 0) throw new ConflictException("Є активні покупки");
      productRepository.archiveAllProductsByMerchantId(storeId);
    }

    store.setStatus(newStatus.toUpperCase());
    return mapToDto(storeRepository.save(store));
  }

  public org.springframework.data.domain.Page<StoreResponseDto> getAllStores(String keyword, int page, int size, String sortBy, String sortDir) {
    org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc")
      ? org.springframework.data.domain.Sort.by(sortBy).ascending() : org.springframework.data.domain.Sort.by(sortBy).descending();
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

    org.springframework.data.domain.Page<Store> stores = (keyword != null && !keyword.trim().isEmpty())
      ? storeRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable) : storeRepository.findAll(pageable);

    return stores.map(this::mapToDto);
  }

  @Transactional
  public void deleteStore(Long storeId, Long requesterId) {
    Store store = storeRepository.findById(storeId).orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));
    User requester = userRepository.findById(requesterId).orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    if (!"ADMIN".equalsIgnoreCase(requester.getRole()) && !store.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Ви не маєте прав видаляти цей магазин");
    }
    if (productRepository.existsByMerchantId(storeId)) {
      throw new ConflictException("Неможливо видалити магазин: є товари.");
    }

    User owner = userRepository.findById(store.getOwnerId()).orElseThrow(() -> new ResourceNotFoundException("Власника не знайдено"));
    if ("MERCHANT".equalsIgnoreCase(owner.getRole())) {
      owner.setRole("BUYER");
      userRepository.save(owner);
    }

    // --- ДОДАНО ОЧИЩЕННЯ РЕКВІЗИТІВ ПРИ ВИДАЛЕННІ МАГАЗИНУ ---
    merchantRequisiteRepository.deleteAllByOwnerId(store.getOwnerId());

    storeRepository.delete(store);
  }

  private String generateSlug(String name) {
    return name.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "").replace(" ", "-");
  }

  private StoreResponseDto mapToDto(Store store) {
    List<StoreShippingMethodDto> shippingDtos = store.getShippingMethods().stream()
      .map(m -> new StoreShippingMethodDto(m.getCarrier().getId(), m.getIsEnabled()))
      .toList();

    return new StoreResponseDto(
      store.getId(),
      store.getOwnerId(),
      store.getName(),
      store.getSlug(),
      store.getAddress(),
      store.getContactPhone(),
      store.getDescription(),
      store.getLogoUrl(),
      store.getRating(),
      store.getStatus(),
      store.getCreatedAt(),
      store.getUpdatedAt(),
      shippingDtos
    );
  }
}
