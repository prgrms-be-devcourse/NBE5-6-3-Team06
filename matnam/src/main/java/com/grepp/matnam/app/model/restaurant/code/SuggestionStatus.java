package com.grepp.matnam.app.model.restaurant.code;

public enum SuggestionStatus {
    APPROVED("승인"),
    REJECTED("거절"),
    PENDING("대기");

    private final String koreanName;

    SuggestionStatus(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
