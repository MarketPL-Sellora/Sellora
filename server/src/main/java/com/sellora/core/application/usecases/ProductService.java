package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Category;
import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.entities.Store;
import com.sellora.core.domain.specifications.ProductSpecification;
import com.sellora.core.infrastructure.persistence.CategoryRepository;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.infrastructure.persistence.StoreRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import com.sellora.core.presentation.dtos.ProductResponseDto;
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
    product.setStatus("ACTIVE");
    product.setCategoryId(dto.categoryId());
    product.setMerchantId(currentStore.getId());

    product.setAttributes(dto.attributes() != null ? dto.attributes() : Map.of());
    product.setImages(dto.images() != null ? dto.images() : List.of());

    return productRepository.save(product);
  }

  // Оновлено Double на BigDecimal для точності
  public Page<Product> filterProducts(
    String keyword, BigDecimal minPrice, BigDecimal maxPrice, Long categoryId,
    int page, int size, String sortBy, String sortDir) {

    Specification<Product> spec = Specification.where(ProductSpecification.hasTitle(keyword))
      .and(ProductSpecification.priceGreaterThanOrEqual(minPrice))
      .and(ProductSpecification.priceLessThanOrEqual(maxPrice))
      .and(ProductSpecification.hasCategoryId(categoryId));

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
}
