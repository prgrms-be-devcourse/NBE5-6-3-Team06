package com.grepp.matnam.app.model.coupon.dto;

import com.grepp.matnam.app.model.coupon.code.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class CouponTemplateUpdateDto {

    @NotBlank(message = "쿠폰 이름을 입력해주세요.")
    private String name;

    private String description;

    private DiscountType discountType;

    private Integer discountValue;

    @Min(value = 1, message = "총 발급 수량은 1 이상이어야 합니다.")
    private Integer totalQuantity;

    @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
    private Integer validDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;
}