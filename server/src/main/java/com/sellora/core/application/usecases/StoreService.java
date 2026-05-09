package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.entities.User;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.infrastructure.persistence.UserRepository;
import com.sellora.core.presentation.dtos.CreateStoreRequest;
import com.sellora.core.presentation.dtos.StoreResponseDto;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createStore(CreateStoreRequest request, Long userId) { // Тепер приймаємо Long

        // 1. Знаходимо реального користувача в базі даних
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено в базі даних"));

        // 2. Перевіряємо, чи в юзера вже є магазин
        if (storeRepository.existsByOwnerId(currentUser.getId())) {
            throw new RuntimeException("У вас вже є створений магазин!");
        }

        // 3. Створюємо магазин
        Store store = new Store();
        store.setOwnerId(currentUser.getId());
        store.setName(request.getName());
        // Генерація slug
        store.setSlug(request.getName().toLowerCase().trim().replaceAll("[^a-z0-9 ]", "").replace(" ", "-"));
        store.setAddress(request.getAddress());
        store.setContactPhone(request.getContactPhone());
        store.setDescription(request.getDescription());
        store.setLogoUrl(request.getLogoUrl());
        store.setStatus("PENDING");

        storeRepository.save(store);

        // 4. ОНОВЛЮЄМО РОЛЬ: тепер currentUser точно не null
        currentUser.setRole("MERCHANT");
        userRepository.save(currentUser);
    }

  public StoreResponseDto getStoreByUserId(Long userId) {
    // 1. Шукаємо магазин у базі
    Store store = storeRepository.findByOwnerId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Магазин для цього користувача не знайдено"));

    // 2. Формуємо DTO для відповіді
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
}
