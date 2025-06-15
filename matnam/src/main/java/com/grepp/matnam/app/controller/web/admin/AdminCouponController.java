package com.grepp.matnam.app.controller.web.admin;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.code.DiscountType;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.service.CouponManageService;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.restaurant.service.RestaurantService;
import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.payload.PageParam;
import com.grepp.matnam.infra.response.PageResponse;
import com.grepp.matnam.infra.response.ResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@Slf4j
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponManageService couponManageService;
    private final RestaurantService restaurantService;

    @GetMapping({"", "/", "/list"})
    public String couponManagement(@RequestParam(required = false) CouponTemplateStatus status,
                                   @RequestParam(required = false, defaultValue = "") String keyword,
                                   @RequestParam(required = false, defaultValue = "newest") String sort,
                                   @Valid PageParam param, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            throw new CommonException(ResponseCode.BAD_REQUEST);
        }

        Sort sortOption;
        switch (sort) {
            case "name":
                sortOption = Sort.by("name").ascending();
                break;
            case "restaurant":
                sortOption = Sort.by("restaurant.name").ascending();
                break;
            case "startAt":
                sortOption = Sort.by("startAt").ascending();
                break;
            case "endAt":
                sortOption = Sort.by("endAt").ascending();
                break;
            default:
                sortOption = Sort.by(Sort.Order.desc("createdAt"));
        }

        Pageable pageable = PageRequest.of(param.getPage() - 1, param.getSize(), sortOption);

        String statusName = "";
        if (status != null) {
            statusName = status.name();
        }

        Page<CouponTemplate> page = couponManageService.findByFilter(statusName, keyword, pageable);

        if (param.getPage() != 1 && page.getContent().isEmpty()) {
            throw new CommonException(ResponseCode.BAD_REQUEST);
        }

        PageResponse<CouponTemplate> response = new PageResponse<>(
                "/admin/coupons/list?status=" + statusName + "&keyword=" + keyword + "&sort=" + sort,
                page, 5
        );

        List<Restaurant> restaurants = restaurantService.findAllActive();

        model.addAttribute("pageTitle", "쿠폰 관리");
        model.addAttribute("currentPage", "coupon-management");
        model.addAttribute("page", response);
        model.addAttribute("status", statusName);
        model.addAttribute("sort", sort);
        model.addAttribute("restaurants", restaurants);
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("couponStatuses", CouponTemplateStatus.values());

        return "admin/coupon-management";
    }
}