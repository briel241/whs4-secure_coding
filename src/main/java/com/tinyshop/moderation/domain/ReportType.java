package com.tinyshop.moderation.domain;

public enum ReportType {
    FRAUD("사기 의심"),
    PROHIBITED_ITEM("금지 품목"),
    HARASSMENT("욕설 또는 괴롭힘"),
    SPAM("스팸"),
    PERSONAL_INFO("개인정보 노출"),
    OTHER("기타");

    private final String label;

    ReportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
