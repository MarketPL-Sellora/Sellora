package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.infrastructure.persistence.ProductRepository;
import com.sellora.core.presentation.dtos.CreateProductDto;
import lombok.RequiredArgsConstructor;
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
    product.setStatus("active");

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
}
