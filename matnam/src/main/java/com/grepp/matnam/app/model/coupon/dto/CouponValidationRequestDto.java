package com.grepp.matnam.app.model.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CouponValidationRequestDto {

    @NotBlank(message = "쿠폰 코드를 입력해주세요.")
    private String couponCode;
}