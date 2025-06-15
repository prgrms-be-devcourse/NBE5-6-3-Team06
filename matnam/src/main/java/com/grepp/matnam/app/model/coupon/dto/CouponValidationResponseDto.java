package com.grepp.matnam.app.model.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponValidationResponseDto {
    private boolean isValid;
    private String message;
    private String couponName;
    private String discountInfo;
}