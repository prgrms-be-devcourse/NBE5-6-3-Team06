package com.grepp.matnam.app.controller.web.user;

import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.service.CouponManageService;
import com.grepp.matnam.app.model.coupon.service.UserCouponService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.payload.PageParam;
import com.grepp.matnam.infra.response.PageResponse;
import com.grepp.matnam.infra.response.ResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.Set;

@Controller
@Slf4j
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponListController {

    private final CouponManageService couponManageService;
    private final UserCouponService userCouponService;

    private static final int COUPON_PAGE_SIZE = 6;

    @GetMapping
    public String couponList(@RequestParam(required = false, defaultValue = "") String keyword,
                             @RequestParam(required = false, defaultValue = "default") String sort,
                             @Valid PageParam param, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            throw new CommonException(ResponseCode.BAD_REQUEST);
        }

        Sort sortOption = null;
        Page<CouponTemplate> page;

        switch (sort) {
            case "default":
                page = couponManageService.findAllActiveCouponsWithDefaultSort(keyword,
                        PageRequest.of(param.getPage() - 1, COUPON_PAGE_SIZE));
                break;
            case "name":
                sortOption = Sort.by("name").ascending();
                page = couponManageService.findAllActiveCoupons(keyword,
                        PageRequest.of(param.getPage() - 1, COUPON_PAGE_SIZE, sortOption));
                break;
            case "restaurant":
                sortOption = Sort.by("restaurant.name").ascending();
                page = couponManageService.findAllActiveCoupons(keyword,
                        PageRequest.of(param.getPage() - 1, COUPON_PAGE_SIZE, sortOption));
                break;
            case "endAt":
                sortOption = Sort.by("endAt").ascending();
                page = couponManageService.findAllActiveCoupons(keyword,
                        PageRequest.of(param.getPage() - 1, COUPON_PAGE_SIZE, sortOption));
                break;
            default:
                sortOption = Sort.by(Sort.Order.desc("createdAt"));
                page = couponManageService.findAllActiveCoupons(keyword,
                        PageRequest.of(param.getPage() - 1, COUPON_PAGE_SIZE, sortOption));
                break;
        }

        if (param.getPage() != 1 && page.getContent().isEmpty()) {
            throw new CommonException(ResponseCode.BAD_REQUEST);
        }

        PageResponse<CouponTemplate> response = new PageResponse<>(
                "/coupons?keyword=" + keyword + "&sort=" + sort,
                page, 6
        );

        Set<Long> appliedCouponIds = new HashSet<>();
        try {
            if (AuthenticationUtils.getCurrentUserId() != null) {
                String userId = AuthenticationUtils.getCurrentUserId();
                appliedCouponIds = userCouponService.getUserAppliedCouponTemplateIds(userId);
            }
        } catch (Exception e) {
            // 비로그인 상태인 경우 빈 Set
        }

        model.addAttribute("pageTitle", "쿠폰 받기");
        model.addAttribute("page", response);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("appliedCouponIds", appliedCouponIds);

        return "user/coupon-list";
    }
}