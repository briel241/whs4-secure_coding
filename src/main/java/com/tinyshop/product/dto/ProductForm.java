package com.tinyshop.product.dto;

import com.tinyshop.product.domain.ItemCondition;
import com.tinyshop.product.domain.Product;
import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.format.annotation.NumberFormat;

public class ProductForm {
    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "제목은 2~100자여야 합니다.")
    private String title;

    @NotBlank(message = "설명을 입력해 주세요.")
    @Size(min = 1, max = 5000, message = "설명은 1~5,000자여야 합니다.")
    private String description;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    @Max(value = 100000000, message = "가격은 100,000,000 이하여야 합니다.")
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private long price;

    @NotNull(message = "카테고리를 선택해 주세요.")
    private UUID categoryId;

    @NotNull(message = "상품 상태를 선택해 주세요.")
    private ItemCondition itemCondition;

    @NotNull(message = "거래 방식을 선택해 주세요.")
    private TradeMethod tradeMethod;

    @Size(max = 100, message = "거래 지역은 100자 이하여야 합니다.")
    private String tradeRegion;

    @NotNull(message = "판매 상태를 선택해 주세요.")
    private SaleStatus saleStatus = SaleStatus.ON_SALE;

    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.title = product.getTitle();
        form.description = product.getDescription();
        form.price = product.getPrice();
        form.categoryId = product.getCategory().getId();
        form.itemCondition = product.getItemCondition();
        form.tradeMethod = product.getTradeMethod();
        form.tradeRegion = product.getTradeRegion();
        form.saleStatus = product.getSaleStatus();
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public ItemCondition getItemCondition() { return itemCondition; }
    public void setItemCondition(ItemCondition itemCondition) { this.itemCondition = itemCondition; }
    public TradeMethod getTradeMethod() { return tradeMethod; }
    public void setTradeMethod(TradeMethod tradeMethod) { this.tradeMethod = tradeMethod; }
    public String getTradeRegion() { return tradeRegion; }
    public void setTradeRegion(String tradeRegion) { this.tradeRegion = tradeRegion; }
    public SaleStatus getSaleStatus() { return saleStatus; }
    public void setSaleStatus(SaleStatus saleStatus) { this.saleStatus = saleStatus; }
}
