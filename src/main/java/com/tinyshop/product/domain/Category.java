package com.tinyshop.product.domain;

import com.tinyshop.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 50)
    private String normalizedName;

    @Column(nullable = false)
    private boolean active;

    protected Category() {
    }

    public Category(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.active = true;
    }

    public void update(String name, String normalizedName, boolean active) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public boolean isActive() {
        return active;
    }
}
