package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.specifications.ProductSpecification;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;

  @Transactional
  public Product createProduct(CreateProductDto dto) {
    Product product = new Product();
    product.setTitle(dto.title());
    product.setStandardPrice(dto.standardPrice());
    product.setStockQuantity(dto.stockQuantity());
    product.setStatus("ACTIVE");

    product.setMerchantId(1L);
    product.setCategoryId(2L);

    product.setAttributes("{}");
    product.setImages("[]");
    product.setGroupPrice(dto.standardPrice());
    product.setGroupTargetSize(2);
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
