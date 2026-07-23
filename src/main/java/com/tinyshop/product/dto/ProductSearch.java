package com.tinyshop.product.dto;

import com.tinyshop.product.domain.SaleStatus;
import com.tinyshop.product.domain.TradeMethod;
import java.util.UUID;
import org.springframework.format.annotation.NumberFormat;

public class ProductSearch {
    private String query;
    private UUID category;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Long minPrice;
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private Long maxPrice;
    private SaleStatus status;
    private TradeMethod tradeMethod;
    private String sort = "latest";
    private int page = 0;
    private int size = 20;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public UUID getCategory() { return category; }
    public void setCategory(UUID category) { this.category = category; }
    public Long getMinPrice() { return minPrice; }
    public void setMinPrice(Long minPrice) { this.minPrice = minPrice; }
    public Long getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Long maxPrice) { this.maxPrice = maxPrice; }
    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status; }
    public TradeMethod getTradeMethod() { return tradeMethod; }
    public void setTradeMethod(TradeMethod tradeMethod) { this.tradeMethod = tradeMethod; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
