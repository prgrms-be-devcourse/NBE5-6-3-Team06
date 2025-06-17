package com.grepp.matnam.app.model.coupon.dto;

import com.grepp.matnam.app.model.coupon.code.CouponStatus;
import com.grepp.matnam.app.model.coupon.code.DiscountType;
import com.grepp.matnam.app.model.coupon.entity.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserCouponResponseDto {
    private Long id;
    private String restaurantName;
    private String couponName;
    private String couponCode;
    private DiscountType discountType;
    private int discountValue;
    private CouponStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public static UserCouponResponseDto from(UserCoupon userCoupon) {
        return UserCouponResponseDto.builder()
                .id(userCoupon.getId())
                .restaurantName(userCoupon.getCouponTemplate().getRestaurant().getName())
                .couponName(userCoupon.getCouponTemplate().getName())
                .couponCode(userCoupon.getCouponCode())
                .discountType(userCoupon.getCouponTemplate().getDiscountType())
                .discountValue(userCoupon.getCouponTemplate().getDiscountValue())
                .status(userCoupon.getStatus())
                .issuedAt(userCoupon.getIssuedAt())
                .expiresAt(userCoupon.getExpiresAt())
                .build();
    }
}