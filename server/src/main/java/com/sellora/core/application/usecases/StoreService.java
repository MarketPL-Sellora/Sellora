package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreResponseDto;
import com.sellora.core.presentation.dtos.UpdateStoreRequest;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StoreService {

  private final StoreRepository storeRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final GroupBuySessionRepository groupBuySessionRepository; // <--- ДОДАНО
  private final ImageUploadService imageUploadService;

  @Transactional
  public void createStore(CreateStoreRequest request, Long userId) {
    User currentUser = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено в базі даних"));

    // BUG-005 FIX: Викидаємо ConflictException замість RuntimeException
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

    // BUG-001 FIX: Просто зберігаємо URL, який прислав фронтенд
    store.setLogoUrl(request.getLogoUrl());

    store.setStatus("PENDING");

    storeRepository.save(store);

    // BUG-003 FIX: Змінюємо роль на MERCHANT, ТІЛЬКИ якщо юзер НЕ є Адміном
    if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
      currentUser.setRole("MERCHANT");
      userRepository.save(currentUser);
    }
  }

  public StoreResponseDto getStoreByUserId(Long userId) {
    Store store = storeRepository.findByOwnerId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин для цього користувача не знайдено"));

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
      if (storeRepository.existsByNameAndIdNot(request.name(), storeId)) {
        throw new ConflictException("Магазин з такою назвою вже існує");
      }
      String newSlug = generateSlug(request.name());
      if (storeRepository.existsBySlugAndIdNot(newSlug, storeId)) {
        throw new ConflictException("Згенерований slug вже зайнятий іншим магазином");
      }
      store.setName(request.name());
      store.setSlug(newSlug);
    }

    store.setAddress(request.address());
    store.setContactPhone(request.contactPhone());
    store.setDescription(request.description());
    store.setLogoUrl(request.logoUrl());

    Store updatedStore = storeRepository.save(store);

    return mapToDto(updatedStore);
  }

  // Допоміжний метод для генерації slug
  private String generateSlug(String name) {
    return name.toLowerCase().trim().replaceAll("[^a-z0-9 ]", "").replace(" ", "-");
  }

  // Допоміжний метод для перетворення Entity в DTO
  private StoreResponseDto mapToDto(Store store) {
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
      store.getUpdatedAt()
    );
  }

  @Transactional
  public StoreResponseDto updateStoreStatus(Long storeId, String newStatus, Long userId) {
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    User currentUser = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено"));

    boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
    boolean isOwner = store.getOwnerId().equals(userId);
    String oldStatus = store.getStatus();

    if (!isAdmin && !isOwner) {
      throw new ForbiddenException("Ви не маєте прав змінювати статус цього магазину");
    }

    if (("PENDING".equalsIgnoreCase(oldStatus) || "BLOCKED".equalsIgnoreCase(oldStatus)) && !isAdmin) {
      throw new ForbiddenException("Тільки адміністратор може змінювати статус з " + oldStatus);
    }

    // --- ЛОГІКА БЛОКУВАННЯ АДМІНОМ ---
    if ("BLOCKED".equalsIgnoreCase(newStatus)) {
      if (!isAdmin) throw new ForbiddenException("Тільки адміністратор може заблокувати магазин");

      productRepository.archiveAllProductsByMerchantId(storeId); // 1. Ховаємо товари
      groupBuySessionRepository.cancelAllActiveSessionsForMerchant(storeId); // 2. Скасовуємо сесії (Гроші не збираємо)
    }
    else if ("BLOCKED".equalsIgnoreCase(oldStatus) && "ACTIVE".equalsIgnoreCase(newStatus)) {
      if (!isAdmin) throw new ForbiddenException("Тільки адміністратор може розблокувати магазин");
    }

    // --- ЛОГІКА ЗАКРИТТЯ ВЛАСНИКОМ ---
    if ("CLOSED".equalsIgnoreCase(newStatus)) {
      if (!isOwner) throw new ForbiddenException("Тільки власник може закрити свій магазин");

      // Захист: Власник не може закритися, якщо має активні сесії
      if (groupBuySessionRepository.countActiveSessionsForMerchant(storeId) > 0) {
        throw new ConflictException("Неможливо закрити магазин: у вас є активні групові покупки. Дочекайтеся їх завершення або скасуйте їх.");
      }

      productRepository.archiveAllProductsByMerchantId(storeId); // Ховаємо товари
    }
    else if ("CLOSED".equalsIgnoreCase(oldStatus) && "ACTIVE".equalsIgnoreCase(newStatus)) {
      if (!isOwner) throw new ForbiddenException("Тільки власник може відкрити свій магазин");
    }

    store.setStatus(newStatus.toUpperCase());
    Store updatedStore = storeRepository.save(store);

    return mapToDto(updatedStore);
  }

  public org.springframework.data.domain.Page<StoreResponseDto> getAllStores(
    String keyword, int page, int size, String sortBy, String sortDir) {

    org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase(org.springframework.data.domain.Sort.Direction.ASC.name())
      ? org.springframework.data.domain.Sort.by(sortBy).ascending()
      : org.springframework.data.domain.Sort.by(sortBy).descending();

    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

    org.springframework.data.domain.Page<Store> stores;
    if (keyword != null && !keyword.trim().isEmpty()) {
      stores = storeRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
    } else {
      stores = storeRepository.findAll(pageable);
    }

    // Spring Data Page автоматично підтримує map() для перетворення Entity в DTO
    return stores.map(this::mapToDto);
  }

  // --- МЕТОД ДЛЯ DELETE /api/v1/stores/{storeId} ---
  @Transactional
  public void deleteStore(Long storeId, Long requesterId) {
    // 1. Обов'язкова перевірка чи store_id існує
    Store store = storeRepository.findById(storeId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин не знайдено"));

    // 2. Перевірка хто відправив запит
    User requester = userRepository.findById(requesterId)
      .orElseThrow(() -> new ResourceNotFoundException("Користувача (який робить запит) не знайдено"));

    boolean isAdmin = "ADMIN".equalsIgnoreCase(requester.getRole());
    boolean isOwner = store.getOwnerId().equals(requesterId);

    if (!isAdmin && !isOwner) {
      throw new ForbiddenException("Ви не маєте прав видаляти цей магазин");
    }

    // 3. Перевірка на відсутність БУДЬ-ЯКИХ products
    if (productRepository.existsByMerchantId(storeId)) {
      throw new ConflictException("Неможливо видалити магазин: до нього прив'язані товари. Спочатку видаліть всі товари.");
    }

    // 4. (ВАЖЛИВО) Робота з ролями
    // Знаходимо ВЛАСНИКА магазину (це може бути той самий requester, а може бути інший юзер, якщо видаляє ADMIN)
    User owner = userRepository.findById(store.getOwnerId())
      .orElseThrow(() -> new ResourceNotFoundException("Власника магазину не знайдено"));

    // Якщо власник був MERCHANT, переводимо в BUYER. Якщо він ADMIN - залишаємо ADMIN.
    if ("MERCHANT".equalsIgnoreCase(owner.getRole())) {
      owner.setRole("BUYER");
      userRepository.save(owner);
    }

    // 5. Видалення
    storeRepository.delete(store);
  }
}
