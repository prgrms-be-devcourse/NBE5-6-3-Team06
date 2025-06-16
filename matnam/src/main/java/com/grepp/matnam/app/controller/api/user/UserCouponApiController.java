package com.grepp.matnam.app.controller.api.user;

import com.grepp.matnam.app.model.coupon.dto.CouponValidationResponseDto;
import com.grepp.matnam.app.model.coupon.dto.UserCouponResponseDto;
import com.grepp.matnam.app.model.coupon.service.CouponIssueService;
import com.grepp.matnam.app.model.coupon.service.CouponValidationService;
import com.grepp.matnam.app.model.coupon.service.UserCouponService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.response.ApiResponse;
import com.grepp.matnam.infra.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 쿠폰")
@RestController
@RequestMapping("/api/user/coupons")
@RequiredArgsConstructor
public class UserCouponApiController {

    private final CouponIssueService couponIssueService;
    private final UserCouponService userCouponService;
    private final CouponValidationService couponValidationService;

    @Operation(summary = "선착순 쿠폰 발급 신청")
    @PostMapping("/{templateId}/apply")
    public ApiResponse<String> applyForCoupon(@PathVariable Long templateId) {
        String userId = AuthenticationUtils.getCurrentUserId();
        couponIssueService.applyForCoupon(userId, templateId);
        return ApiResponse.success("쿠폰 신청이 완료되었습니다. 선착순 결과는 잠시 후 확인 가능합니다.");
    }

    @Operation(summary = "내 쿠폰 목록 조회")
    @GetMapping("/my")
    public ApiResponse<List<UserCouponResponseDto>> getMyCoupons() {
        String userId = AuthenticationUtils.getCurrentUserId();
        List<UserCouponResponseDto> myCoupons = userCouponService.getMyCoupons(userId);
        return ApiResponse.success(myCoupons);
    }

    @Operation(summary = "내 쿠폰 사용")
    @PostMapping("/{couponCode}/use")
    public ResponseEntity<ApiResponse<CouponValidationResponseDto>> useMyCoupon(@PathVariable String couponCode) {
        String userId = AuthenticationUtils.getCurrentUserId();
        CouponValidationResponseDto result = couponValidationService.validateAndUseCouponByUser(couponCode, userId);

        if (result.isValid()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.status(ResponseCode.INVALID_COUPON.status())
                    .body(ApiResponse.error(ResponseCode.INVALID_COUPON));
        }
    }
}