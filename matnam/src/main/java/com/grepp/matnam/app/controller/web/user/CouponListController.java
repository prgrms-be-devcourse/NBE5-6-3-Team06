package com.grepp.matnam.app.controller.web.user;

import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.service.CouponManageService;
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

@Controller
@Slf4j
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponListController {

    private final CouponManageService couponManageService;

    @GetMapping
    public String couponList(@RequestParam(required = false, defaultValue = "") String keyword,
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
            case "endAt":
                sortOption = Sort.by("endAt").ascending(); // 마감 임박순
                break;
            default:
                sortOption = Sort.by(Sort.Order.desc("createdAt")); // 최신순
        }

        Pageable pageable = PageRequest.of(param.getPage() - 1, param.getSize(), sortOption);

        Page<CouponTemplate> page = couponManageService.findAvailableCoupons(keyword, pageable);

        if (param.getPage() != 1 && page.getContent().isEmpty()) {
            throw new CommonException(ResponseCode.BAD_REQUEST);
        }

        PageResponse<CouponTemplate> response = new PageResponse<>(
                "/coupons?keyword=" + keyword + "&sort=" + sort,
                page, 5
        );

        model.addAttribute("pageTitle", "쿠폰 받기");
        model.addAttribute("page", response);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "user/coupon-list";
    }
}