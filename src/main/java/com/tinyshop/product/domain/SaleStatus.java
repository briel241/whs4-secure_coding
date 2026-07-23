package com.tinyshop.product.domain;

public enum SaleStatus {
    DRAFT("임시저장"), ON_SALE("판매 중"), RESERVED("예약 중"), SOLD("판매 완료"), DELETED("삭제됨");

    private final String label;

    SaleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
