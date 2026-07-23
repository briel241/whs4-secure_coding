package com.tinyshop.product.repository;

import com.tinyshop.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);
    Page<Product> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);
}
