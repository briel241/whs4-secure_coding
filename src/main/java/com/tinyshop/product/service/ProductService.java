package com.tinyshop.product.service;

import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.product.domain.Category;
import com.tinyshop.product.domain.ModerationStatus;
import com.tinyshop.product.domain.Product;
import com.tinyshop.product.domain.ProductImage;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.product.dto.ProductSearch;
import com.tinyshop.product.repository.CategoryRepository;
import com.tinyshop.product.repository.ProductImageRepository;
import com.tinyshop.product.repository.ProductRepository;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.service.UserService;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {
    private static final Set<SaleStatus> PUBLIC_STATUSES = Set.of(
        SaleStatus.ON_SALE, SaleStatus.RESERVED, SaleStatus.SOLD);
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductImageRepository images;
    private final UserService users;
    private final ImageStorageService imageStorage;

    public ProductService(ProductRepository products, CategoryRepository categories,
                          ProductImageRepository images, UserService users,
                          ImageStorageService imageStorage) {
        this.products = products;
        this.categories = categories;
        this.images = images;
        this.users = users;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearch search) {
        int page = Math.max(0, search.getPage());
        int size = Math.max(1, Math.min(50, search.getSize()));
        String sortKey = Set.of("latest", "priceAsc", "priceDesc").contains(search.getSort())
            ? search.getSort() : "latest";
        Sort sort = switch (sortKey) {
            case "priceAsc" -> Sort.by("price").ascending().and(Sort.by("createdAt").descending());
            case "priceDesc" -> Sort.by("price").descending().and(Sort.by("createdAt").descending());
            default -> Sort.by("createdAt").descending();
        };
        Specification<Product> spec = (root, query, cb) -> {
            var seller = root.join("seller", JoinType.INNER);
            var predicate = cb.and(
                root.get("saleStatus").in(PUBLIC_STATUSES),
                cb.equal(root.get("moderationStatus"), ModerationStatus.VISIBLE),
                cb.equal(seller.get("status"), UserStatus.ACTIVE));
            String keyword = search.getQuery() == null ? "" : search.getQuery().strip();
            if (keyword.length() > 100) throw new BusinessException("검색어는 100자 이하여야 합니다.");
            if (!keyword.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")),
                    "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            if (search.getCategory() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), search.getCategory()));
            }
            if (search.getMinPrice() != null) {
                if (search.getMinPrice() < 0) throw new BusinessException("최소 가격은 0 이상이어야 합니다.");
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), search.getMinPrice()));
            }
            if (search.getMaxPrice() != null) {
                if (search.getMaxPrice() < 0) throw new BusinessException("최대 가격은 0 이상이어야 합니다.");
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), search.getMaxPrice()));
            }
            if (search.getStatus() != null && PUBLIC_STATUSES.contains(search.getStatus())) {
                predicate = cb.and(predicate, cb.equal(root.get("saleStatus"), search.getStatus()));
            }
            if (search.getTradeMethod() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("tradeMethod"), search.getTradeMethod()));
            }
            return predicate;
        };
        return products.findAll(spec, PageRequest.of(page, size, sort));
    }

    @Transactional(readOnly = true)
    public Product detail(UUID id, UUID viewerId) {
        Product product = products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        boolean owner = viewerId != null && product.getSeller().getId().equals(viewerId);
        boolean publiclyVisible = PUBLIC_STATUSES.contains(product.getSaleStatus())
            && product.getModerationStatus() == ModerationStatus.VISIBLE
            && product.getSeller().getStatus() == UserStatus.ACTIVE;
        if (!owner && !publiclyVisible) throw new NotFoundException("Product not found");
        return product;
    }

    @Transactional(readOnly = true)
    public List<ProductImage> images(UUID productId) {
        return images.findByProductIdOrderByDisplayOrder(productId);
    }

    @Transactional
    public Product create(UUID sellerId, ProductForm form, List<MultipartFile> files) {
        Category category = activeCategory(form.getCategoryId());
        Product product = products.save(new Product(users.require(sellerId), category, form));
        imageStorage.store(product, files);
        return product;
    }

    @Transactional
    public Product update(UUID productId, UUID sellerId, ProductForm form, List<MultipartFile> files) {
        Product product = products.findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        if (product.getSaleStatus() == SaleStatus.DELETED) throw new NotFoundException("Product not found");
        product.update(activeCategory(form.getCategoryId()), form);
        imageStorage.store(product, files);
        return product;
    }

    @Transactional
    public void changeStatus(UUID productId, UUID sellerId, SaleStatus status) {
        if (status == null || status == SaleStatus.DELETED) throw new BusinessException("허용하지 않는 상태입니다.");
        Product product = products.findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        product.changeSaleStatus(status);
    }

    @Transactional
    public void delete(UUID productId, UUID sellerId) {
        Product product = products.findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        product.changeSaleStatus(SaleStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public Page<Product> mine(UUID sellerId, int page) {
        return products.findBySellerIdOrderByCreatedAtDesc(sellerId,
            PageRequest.of(Math.max(0, page), 20));
    }

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categories.findByActiveTrueOrderByName();
    }

    private Category activeCategory(UUID id) {
        return categories.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new BusinessException("사용 가능한 카테고리를 선택해 주세요."));
    }
}
