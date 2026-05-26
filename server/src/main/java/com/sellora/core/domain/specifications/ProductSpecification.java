package com.sellora.core.domain.specifications;

import com.sellora.core.domain.entities.Product;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

public class ProductSpecification {

  public static Specification<Product> hasTitle(String keyword) {
    return (root, query, cb) -> keyword == null ? null :
      cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
  }

  public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
    return (root, query, cb) -> minPrice == null ? null :
      cb.greaterThanOrEqualTo(root.get("standardPrice"), minPrice);
  }

  public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
    return (root, query, cb) -> maxPrice == null ? null :
      cb.lessThanOrEqualTo(root.get("standardPrice"), maxPrice);
  }

  public static Specification<Product> hasCategoryId(Long categoryId) {
    return (root, query, cb) -> categoryId == null ? null :
      cb.equal(root.get("categoryId"), categoryId);
  }

  // --- НОВІ СПЕЦИФІКАЦІЇ ---

  public static Specification<Product> hasStatus(String status) {
    return (root, query, cb) -> status == null ? null :
      cb.equal(root.get("status"), status);
  }

  public static Specification<Product> hasStoreId(Long storeId) {
    return (root, query, cb) -> storeId == null ? null :
      cb.equal(root.get("merchantId"), storeId);
  }

  public static Specification<Product> hasGroupSession(String mode) {
    return (root, query, cb) -> {
      if (mode == null || mode.equalsIgnoreCase("ALL")) {
        return null; // Виводимо всі товари
      }

      if (mode.equalsIgnoreCase("ONLY_GROUP")) {
        return cb.and(
          cb.isNotNull(root.get("groupPrice")),
          cb.isNotNull(root.get("groupTargetSize"))
        );
      }

      if (mode.equalsIgnoreCase("WITHOUT_GROUP")) {
        return cb.or(
          cb.isNull(root.get("groupPrice")),
          cb.isNull(root.get("groupTargetSize"))
        );
      }

      return null;
    };
  }
}
