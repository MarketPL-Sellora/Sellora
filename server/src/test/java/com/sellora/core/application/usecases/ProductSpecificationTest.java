package com.sellora.core.application.usecases;

import com.sellora.core.domain.entities.Product;
import com.sellora.core.domain.specifications.ProductSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductSpecificationTest {

  @Mock
  private Root<Product> root;

  @Mock
  private CriteriaQuery<?> query;

  @Mock
  private CriteriaBuilder cb;

  @Mock
  private Path<Object> path;

  @BeforeEach
  void setUp() {
    lenient().when(root.get(anyString())).thenReturn(path);
  }

  @Test
  void hasTitle_KeywordProvided_CallsLikeWithLower() {
    String keyword = "Laptop";
    Specification<Product> spec = ProductSpecification.hasTitle(keyword);

    spec.toPredicate(root, query, cb);

    verify(cb).lower(any());
    verify(cb).like(any(), eq("%laptop%"));
  }

  @Test
  void priceGreaterThanOrEqual_ValueProvided_CallsGreaterThanOrEqualTo() {
    BigDecimal minPrice = BigDecimal.valueOf(100);
    Specification<Product> spec = ProductSpecification.priceGreaterThanOrEqual(minPrice);

    spec.toPredicate(root, query, cb);

    verify(cb).greaterThanOrEqualTo(any(), eq(minPrice));
  }

  @Test
  void hasGroupSession_OnlyGroupMode_CallsAndWithIsNotNull() {
    Specification<Product> spec = ProductSpecification.hasGroupSession("ONLY_GROUP");

    spec.toPredicate(root, query, cb);

    verify(cb, times(2)).isNotNull(any());
    verify(cb).and(any(), any());
  }

  @Test
  void hasGroupSession_WithoutGroupMode_CallsOrWithIsNull() {
    Specification<Product> spec = ProductSpecification.hasGroupSession("WITHOUT_GROUP");

    spec.toPredicate(root, query, cb);

    verify(cb, times(2)).isNull(any());
    verify(cb).or(any(), any());
  }

  @Test
  void specifications_NullValues_ReturnNull() {
    assertNull(ProductSpecification.hasTitle(null).toPredicate(root, query, cb));
    assertNull(ProductSpecification.priceLessThanOrEqual(null).toPredicate(root, query, cb));
    assertNull(ProductSpecification.hasCategoryId(null).toPredicate(root, query, cb));
    assertNull(ProductSpecification.hasGroupSession("ALL").toPredicate(root, query, cb));
  }

  @Test
  void hasStoreId_ValueProvided_CallsEqual() {
    Long storeId = 10L;
    Specification<Product> spec = ProductSpecification.hasStoreId(storeId);

    spec.toPredicate(root, query, cb);

    verify(root).get("merchantId");
    verify(cb).equal(any(), eq(storeId));
  }
}
