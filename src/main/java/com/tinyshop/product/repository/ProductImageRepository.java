package com.tinyshop.product.repository;

import com.tinyshop.product.domain.ProductImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdOrderByDisplayOrder(UUID productId);
    Optional<ProductImage> findByStorageKey(String storageKey);
    long countByProductId(UUID productId);
}
