package com.sellora.core.domain.specifications;

import com.sellora.core.domain.entities.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

  // 1. Фільтр за назвою (замінить наш старий метод)
  public static Specification<Product> hasTitle(String keyword) {
    return (root, query, criteriaBuilder) -> {
      if (keyword == null || keyword.trim().isEmpty()) {
        return criteriaBuilder.conjunction(); // Це означає "ігнорувати цей фільтр" (WHERE 1=1)
      }
      return criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    };
  }

  // 2. Фільтр: Ціна ВІД (мінімальна)
  public static Specification<Product> priceGreaterThanOrEqual(Double minPrice) {
    return (root, query, criteriaBuilder) -> {
      if (minPrice == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.greaterThanOrEqualTo(root.get("standardPrice"), minPrice);
    };
  }

  // 3. Фільтр: Ціна ДО (максимальна)
  public static Specification<Product> priceLessThanOrEqual(Double maxPrice) {
    return (root, query, criteriaBuilder) -> {
      if (maxPrice == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.lessThanOrEqualTo(root.get("standardPrice"), maxPrice);
    };
  }

  // 4. Фільтр за категорією
  public static Specification<Product> hasCategoryId(Long categoryId) {
    return (root, query, criteriaBuilder) -> {
      if (categoryId == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("categoryId"), categoryId);
    };
  }
}
