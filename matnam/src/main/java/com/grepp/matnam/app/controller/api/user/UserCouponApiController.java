package com.grepp.matnam.app.controller.api.user;

import com.grepp.matnam.app.model.coupon.dto.UserCouponResponseDto;
import com.grepp.matnam.app.model.coupon.service.CouponIssueService;
import com.grepp.matnam.app.model.coupon.service.UserCouponService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 쿠폰")
@RestController
@RequestMapping("/api/user/coupons")
@RequiredArgsConstructor
public class UserCouponApiController {

    private final CouponIssueService couponIssueService;
    private final UserCouponService userCouponService;

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
}