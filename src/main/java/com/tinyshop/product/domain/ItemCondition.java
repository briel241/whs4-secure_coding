package com.tinyshop.product.domain;

public enum ItemCondition {
    NEW("미사용"), LIKE_NEW("거의 새것"), GOOD("사용감 적음"), FAIR("사용감 있음");

    private final String label;

    ItemCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
