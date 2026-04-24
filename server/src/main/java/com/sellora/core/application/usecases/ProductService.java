package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.specifications.ProductSpecification;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;
  private final StoreRepository storeRepository; // Додаємо репозиторій магазинів

  @Transactional
  public Product createProduct(CreateProductDto dto) {
    // 1. Отримуємо ID поточного користувача з токена
    // Приводимо до Long, оскільки ми зберігали його як Long у JwtAuthenticationFilter
    Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // 2. Знаходимо магазин, який належить цьому користувачу
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new RuntimeException("У вас немає активного магазину. Спочатку створіть магазин!"));

    // 3. Створюємо товар
    Product product = new Product();
    product.setTitle(dto.title());
    product.setDescription(dto.description());
    product.setStandardPrice(dto.standardPrice());
    product.setGroupPrice(dto.groupPrice());
    product.setGroupTargetSize(dto.groupTargetSize());
    product.setStockQuantity(dto.stockQuantity());
    product.setStatus("ACTIVE");

    product.setCategoryId(dto.categoryId());

    // Встановлюємо ID знайденого магазину (колонка store_id у базі)
    product.setMerchantId(currentStore.getId());

    // JSONB поля (Hibernate 6 завдяки @JdbcTypeCode зробить все сам)
    product.setAttributes(dto.attributes() != null ? dto.attributes() : Map.of());
    product.setImages(dto.images() != null ? dto.images() : List.of());

    return productRepository.save(product);
  }

  /**
   * Універсальний метод для фільтрації, пошуку та пагінації товарів.
   */
  public Page<Product> filterProducts(
    String keyword,
    Double minPrice,
    Double maxPrice,
    Long categoryId,
    int page,
    int size,
    String sortBy,
    String sortDir
  ) {
    // 1. Створюємо базову специфікацію (тільки активні товари через статус магазину)
    // Для MVP ми поєднуємо наші нові фільтри
    Specification<Product> spec = Specification.where(ProductSpecification.hasTitle(keyword))
      .and(ProductSpecification.priceGreaterThanOrEqual(minPrice))
      .and(ProductSpecification.priceLessThanOrEqual(maxPrice))
      .and(ProductSpecification.hasCategoryId(categoryId));

    // 2. Налаштовуємо сортування
    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();

    // 3. Створюємо об'єкт пагінації
    Pageable pageable = PageRequest.of(page, size, sort);

    // 4. Повертаємо результат фільтрації
    return productRepository.findAll(spec, pageable);
  }
}
