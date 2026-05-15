package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.specifications.ProductSpecification;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.infrastructure.persistence.GroupBuySessionRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
import com.sellora.core.presentation.exceptions.BadRequestException;
import com.sellora.core.presentation.exceptions.ConflictException;
import com.sellora.core.presentation.exceptions.ForbiddenException;
import com.sellora.core.presentation.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;
  private final CategoryRepository categoryRepository;
  private final GroupBuySessionRepository groupBuySessionRepository; // Додай у шапку класу

  @Transactional
  public Product createProduct(CreateProductDto dto) {
    Long currentUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new RuntimeException("У вас немає активного магазину. Спочатку створіть магазин!"));

    Product product = new Product();
    product.setTitle(dto.title());
    product.setDescription(dto.description());
    product.setStandardPrice(dto.standardPrice());
    product.setGroupPrice(dto.groupPrice());
    product.setGroupTargetSize(dto.groupTargetSize());
    product.setStockQuantity(dto.stockQuantity());
    if (dto.stockQuantity() == 0) {
      product.setStatus("OUT_OF_STOCK");
    } else {
      product.setStatus("ACTIVE");
    }

    product.setCategoryId(dto.categoryId());
    product.setMerchantId(currentStore.getId());

    product.setAttributes(dto.attributes() != null ? dto.attributes() : Map.of());
    product.setImages(dto.images() != null ? dto.images() : List.of());

    return productRepository.save(product);
  }

  public Page<Product> filterProducts(
    String keyword, BigDecimal minPrice, BigDecimal maxPrice, Long categoryId,
    String status, Long storeId, String groupMode, // Змінено на String
    int page, int size, String sortBy, String sortDir) {

    Specification<Product> spec = Specification.where(ProductSpecification.hasTitle(keyword))
      .and(ProductSpecification.priceGreaterThanOrEqual(minPrice))
      .and(ProductSpecification.priceLessThanOrEqual(maxPrice))
      .and(ProductSpecification.hasCategoryId(categoryId))
      .and(ProductSpecification.hasStatus(status))
      .and(ProductSpecification.hasStoreId(storeId))
      .and(ProductSpecification.hasGroupSession(groupMode));

    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);
    return productRepository.findAll(spec, pageable);
  }

  // --- НОВИЙ МЕТОД 1: Товари конкретного продавця ---
  public Page<Product> getProductsByMerchant(Long merchantId, int page, int size, String sortBy, String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    return productRepository.findAllByMerchantId(merchantId, pageable);
  }

  // --- НОВИЙ МЕТОД 2: Конкретний товар з параметром full ---
  public ProductResponseDto getProductById(Long id, boolean full) {
    Product product = productRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    String storeName = null;
    String categoryName = null;

    // Якщо фронтенд попросив повну інформацію, робимо додаткові запити до БД
    if (full) {
      storeName = storeRepository.findById(product.getMerchantId())
        .map(Store::getName)
        .orElse(null);

      // Виправлено: звертаємось до змінної categoryRepository (з малої літери)
      categoryName = categoryRepository.findById(product.getCategoryId())
        .map(Category::getName)
        .orElse(null);
    }

    // Збираємо все в красивий DTO
    return new ProductResponseDto(
      product.getId(),
      product.getTitle(),
      product.getDescription(),
      product.getStandardPrice(),
      product.getGroupPrice(),
      product.getGroupTargetSize(),
      product.getStockQuantity(),
      product.getCategoryId(),
      categoryName,
      product.getMerchantId(),
      storeName,
      product.getAttributes(),
      product.getImages(),
      product.getStatus()
    );
  }

  // Додай цей метод в ProductService.java

  @Transactional
  public Product updateProduct(Long productId, com.sellora.core.presentation.dtos.UpdateProductDto dto) {
    // 1. Отримуємо поточного юзера та його магазин (перевірка як при створенні)
    Long currentUserId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    // 2. Шукаємо товар
    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new com.sellora.core.presentation.exceptions.ResourceNotFoundException("Товар не знайдено"));

    // 3. Перевірка: чи належить товар цьому магазину?
    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете редагувати чужий товар");
      // (Заміни на AccessDeniedException або свій кастомний ексепшн, якщо маєш)
    }

    // 4. Перевірка категорії (якщо вона змінилася)
    if (!product.getCategoryId().equals(dto.categoryId())) {
      if (!categoryRepository.existsById(dto.categoryId())) {
        throw new BadRequestException("Вибраної категорії не існує");
      }
    }

    // 5. Оновлюємо базові поля
    product.setTitle(dto.title());
    product.setDescription(dto.description());
    product.setCategoryId(dto.categoryId());
    product.setStandardPrice(dto.standardPrice());
    product.setGroupPrice(dto.groupPrice());
    product.setGroupTargetSize(dto.groupTargetSize());
    product.setImages(dto.images() != null ? dto.images() : java.util.List.of());
    product.setAttributes(dto.attributes() != null ? dto.attributes() : java.util.Map.of());

    // 6. БІЗНЕС-ЛОГІКА: Перевірка запасів та статусу (Згідно з ТЗ)
    int newStock = dto.stockQuantity();
    product.setStockQuantity(newStock);

    if (newStock == 0) {
      // Якщо залишок 0 -> примусово ставимо OUT_OF_STOCK
      product.setStatus("OUT_OF_STOCK");
    } else if (newStock > 0 && "OUT_OF_STOCK".equals(product.getStatus())) {
      // Якщо залишок поповнили і товар БУВ відсутній -> повертаємо ACTIVE
      product.setStatus("ACTIVE");
    }
    // Якщо товар ACTIVE і stock > 0, статус не змінюється

    // 7. Зберігаємо та повертаємо
    return productRepository.save(product);
  }

  @Transactional
  public void deleteProduct(Long productId) {
    // 1. Отримуємо юзера та перевіряємо магазин
    Long currentUserId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    // 2. Шукаємо товар (поверне 404, якщо немає - згідно з ТЗ)
    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    // 3. Сек'юрна перевірка: чи належить товар цьому магазину? (щоб не видалили чуже)
    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете видалити чужий товар");
    }

    // 4. БІЗНЕС-ЛОГІКА: Перевірка на активні сесії групових покупок (поверне 409 - згідно з ТЗ)
    if (groupBuySessionRepository.existsByProductIdAndStatus(productId, "ACTIVE")) {
      throw new ConflictException("Неможливо видалити товар: існують активні сесії групових покупок");
    }

    // 5. Видалення
    productRepository.delete(product);
  }

  // --- НОВИЙ МЕТОД: Точкова зміна статусу (PATCH) ---
  @Transactional
  public Product updateProductStatus(Long productId, String newStatus) {
    // 1. Отримуємо юзера та перевіряємо магазин
    Long currentUserId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Store currentStore = storeRepository.findByOwnerId(currentUserId)
      .orElseThrow(() -> new BadRequestException("У вас немає активного магазину"));

    // 2. Перевірка існування запису (Пункт 1 з ТЗ)
    Product product = productRepository.findById(productId)
      .orElseThrow(() -> new ResourceNotFoundException("Товар не знайдено"));

    // Перевірка власника
    if (!product.getMerchantId().equals(currentStore.getId())) {
      throw new ForbiddenException("Ви не можете редагувати чужий товар");
    }

    // 3. БІЗНЕС-ЛОГІКА: Перевірка статусів
    String oldStatus = product.getStatus();

    if ("ARCHIVED".equalsIgnoreCase(newStatus)) {
      // Пункт 2: Якщо старий Active/OUT_OF_STOCK -> новий Archive, перевірок немає
      product.setStatus("ARCHIVED");

    } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
      // Пункт 3: Якщо старий Archive -> новий Active, перевіряємо stockQuantity
      if (product.getStockQuantity() == 0) {
        throw new BadRequestException("Не можна активувати товар з кількістю 0 на складі");
      }
      product.setStatus("ACTIVE");
    }

    // Зберігаємо і повертаємо оновлений товар
    return productRepository.save(product);
  }
}
