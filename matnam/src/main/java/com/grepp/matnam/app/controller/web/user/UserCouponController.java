package com.grepp.matnam.app.controller.web.user;

import com.grepp.matnam.app.model.coupon.code.CouponStatus;
import com.grepp.matnam.app.model.coupon.dto.UserCouponResponseDto;
import com.grepp.matnam.app.model.coupon.service.UserCouponService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping("/coupons")
    public String myCoupons(Model model) {
        String userId = AuthenticationUtils.getCurrentUserId();
        List<UserCouponResponseDto> allCoupons = userCouponService.getMyCoupons(userId);

        Map<CouponStatus, List<UserCouponResponseDto>> couponsByStatus = allCoupons.stream()
                .collect(Collectors.groupingBy(UserCouponResponseDto::getStatus));

        model.addAttribute("pageTitle", "내 쿠폰");
        model.addAttribute("allCoupons", allCoupons);
        model.addAttribute("availableCoupons", couponsByStatus.getOrDefault(CouponStatus.ISSUED, List.of()));
        model.addAttribute("usedCoupons", couponsByStatus.getOrDefault(CouponStatus.USED, List.of()));
        model.addAttribute("expiredCoupons", couponsByStatus.getOrDefault(CouponStatus.EXPIRED, List.of()));

        return "user/my-coupons";
    }
}