package com.grepp.matnam.app.model.coupon.code;

public enum CouponTemplateStatus {
    ACTIVE,     // 활성 (발급 가능)
    INACTIVE,   // 비활성 (관리자가 중지)
    EXHAUSTED,  // 소진됨 (수량 모두 발급)
    EXPIRED,    // 만료됨 (기간 종료)
    DELETED     // 삭제됨
}