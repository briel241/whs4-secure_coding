package com.tinyshop.product.domain;

import com.tinyshop.common.domain.BaseEntity;
import com.tinyshop.product.dto.ProductForm;
import com.tinyshop.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 20)
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_method", nullable = false, length = 20)
    private TradeMethod tradeMethod;

    @Column(name = "trade_region", length = 100)
    private String tradeRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 20)
    private SaleStatus saleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus;

    @Version
    private long version;

    protected Product() {
    }

    public Product(User seller, Category category, ProductForm form) {
        this.seller = seller;
        this.moderationStatus = ModerationStatus.VISIBLE;
        apply(category, form);
    }

    public void update(Category category, ProductForm form) {
        apply(category, form);
    }

    private void apply(Category category, ProductForm form) {
        this.category = category;
        this.title = form.getTitle().strip();
        this.description = form.getDescription().strip();
        this.price = form.getPrice();
        this.itemCondition = form.getItemCondition();
        this.tradeMethod = form.getTradeMethod();
        this.tradeRegion = form.getTradeRegion() == null ? null : form.getTradeRegion().strip();
        this.saleStatus = form.getSaleStatus();
    }

    public void changeSaleStatus(SaleStatus status) {
        this.saleStatus = status;
    }

    public void changeModerationStatus(ModerationStatus status) {
        this.moderationStatus = status;
    }

    public User getSeller() {
        return seller;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public ItemCondition getItemCondition() {
        return itemCondition;
    }

    public TradeMethod getTradeMethod() {
        return tradeMethod;
    }

    public String getTradeRegion() {
        return tradeRegion;
    }

    public SaleStatus getSaleStatus() {
        return saleStatus;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public long getVersion() {
        return version;
    }
}
