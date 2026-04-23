package com.sellora.core.infrastructure.persistence;

import com.sellora.core.domain.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  // Пошук головних категорій (де parent_id є NULL)
  List<Category> findAllByParentIsNull();
}
