package com.tinyshop.product.domain;

public enum TradeMethod {
    DIRECT("직거래"), DELIVERY("택배"), BOTH("직거래·택배");

    private final String label;

    TradeMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
