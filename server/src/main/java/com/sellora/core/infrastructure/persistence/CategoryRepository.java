package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  // Пошук головних категорій (де parent_id є NULL)
  List<Category> findAllByParentIsNull();

  // Для валідації при створенні та видаленні
  boolean existsByName(String name);
  boolean existsByParentId(Long parentId);

  @Query(value = "WITH RECURSIVE category_tree AS ( " +
    "SELECT id FROM categories WHERE id = :categoryId " +
    "UNION ALL " +
    "SELECT c.id FROM categories c " +
    "INNER JOIN category_tree ct ON c.parent_id = ct.id " +
    ") SELECT id FROM category_tree", nativeQuery = true)
  java.util.List<Long> findCategoryTreeIds(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);
}
