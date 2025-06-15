package com.grepp.matnam.app.controller.api.restaurant;

import com.grepp.matnam.app.model.coupon.dto.CouponValidationRequestDto;
import com.grepp.matnam.app.model.coupon.dto.CouponValidationResponseDto;
import com.grepp.matnam.app.model.coupon.service.CouponValidationService;
import com.grepp.matnam.infra.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "음식점 쿠폰 관리")
@RestController
@RequestMapping("/api/restaurant/coupons")
@RequiredArgsConstructor
public class RestaurantCouponApiController {

    private final CouponValidationService couponValidationService;

    @Operation(summary = "쿠폰 유효성 검증 및 사용 처리")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResponseDto>> validateAndUseCoupon(@Valid @RequestBody CouponValidationRequestDto requestDto) {
        // TODO: 음식점 관계자(점주 등)만 이 API를 호출할 수 있도록 보안 강화 필요
        // 예: 점주 계정의 권한(Role)을 확인하거나, 해당 음식점과 관계된 사용자인지 확인
        CouponValidationResponseDto response = couponValidationService.validateAndUseCoupon(requestDto.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}