package com.grepp.matnam.app.model.coupon.dto;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.code.DiscountType;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponTemplateResponseDto {
    private Long id;
    private String restaurantName;
    private String name;
    private String description;
    private DiscountType discountType;
    private int discountValue;
    private int totalQuantity;
    private int issuedQuantity;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int validDays;
    private CouponTemplateStatus status;

    public static CouponTemplateResponseDto from(CouponTemplate template) {
        return CouponTemplateResponseDto.builder()
                .id(template.getTemplateId())
                .restaurantName(template.getRestaurant().getName())
                .name(template.getName())
                .description(template.getDescription())
                .discountType(template.getDiscountType())
                .discountValue(template.getDiscountValue())
                .totalQuantity(template.getTotalQuantity())
                .issuedQuantity(template.getIssuedQuantity())
                .startAt(template.getStartAt())
                .endAt(template.getEndAt())
                .validDays(template.getValidDays())
                .status(template.getStatus())
                .build();
    }
}