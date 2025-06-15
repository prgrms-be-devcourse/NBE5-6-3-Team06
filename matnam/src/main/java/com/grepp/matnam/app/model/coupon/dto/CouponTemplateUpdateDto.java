package com.grepp.matnam.app.model.coupon.dto;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;
}