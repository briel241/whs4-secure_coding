package com.tinyshop.product.repository;

import com.tinyshop.product.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByActiveTrueOrderByName();
    Optional<Category> findByIdAndActiveTrue(UUID id);
    boolean existsByNormalizedName(String normalizedName);
}
