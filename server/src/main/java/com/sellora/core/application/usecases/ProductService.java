package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // Хардкодимо обов'язкові зв'язки для MVP
    product.setMerchantId(1L);
    product.setCategoryId(2L);

    // Передаємо порожній JSON для цих полів, щоб база не сварилася
    product.setAttributes("{}");
    product.setImages("[]");
    product.setGroupPrice(dto.standardPrice()); // Для MVP групова ціна дорівнює стандартній
    product.setGroupTargetSize(2);
    return productRepository.save(product);
  }

  // НОВИЙ МЕТОД ДЛЯ ПАГІНАЦІЇ
  public Page<Product> getAllProducts(int page, int size, String sortBy, String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
      ? Sort.by(sortBy).ascending()
      : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);
    return productRepository.findAllActiveProducts(pageable);
  }
}
