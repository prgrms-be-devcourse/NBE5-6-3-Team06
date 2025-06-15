package com.grepp.matnam.app.model.coupon.dto;

import com.grepp.matnam.app.model.coupon.code.DiscountType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponTemplateCreateDto {

    @NotNull(message = "음식점을 선택해주세요.")
    private Long restaurantId;

    @NotBlank(message = "쿠폰 이름을 입력해주세요.")
    private String name;

    private String description;

    @NotNull(message = "할인 종류를 선택해주세요.")
    private DiscountType discountType;

    @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
    private int discountValue;

    @Min(value = 1, message = "쿠폰 수량은 1개 이상이어야 합니다.")
    private int totalQuantity;

    @NotNull(message = "발급 시작 시간을 입력해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @NotNull(message = "발급 종료 시간을 입력해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Future(message = "발급 종료 시간은 현재 이후여야 합니다.")
    private LocalDateTime endAt;

    @Min(value = 1, message = "유효 기간은 1일 이상이어야 합니다.")
    private int validDays;
}