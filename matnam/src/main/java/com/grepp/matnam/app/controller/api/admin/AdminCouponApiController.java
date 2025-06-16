package com.grepp.matnam.app.controller.api.admin;

import com.grepp.matnam.app.model.coupon.dto.CouponTemplateCreateDto;
import com.grepp.matnam.app.model.coupon.dto.CouponTemplateResponseDto;
import com.grepp.matnam.app.model.coupon.dto.CouponTemplateUpdateDto;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.service.CouponManageService;
import com.grepp.matnam.infra.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 쿠폰 관리")
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponApiController {

    private final CouponManageService couponManageService;

    @Operation(summary = "쿠폰 템플릿 생성")
    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<CouponTemplateResponseDto>> createCouponTemplate(@Valid @RequestBody CouponTemplateCreateDto createDto) {
        CouponTemplate couponTemplate = couponManageService.createCouponTemplate(createDto);
        return ResponseEntity.ok(ApiResponse.success(CouponTemplateResponseDto.from(couponTemplate)));
    }

    @Operation(summary = "쿠폰 템플릿 상세 조회")
    @GetMapping("/templates/{templateId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<CouponTemplateResponseDto>> getCouponTemplate(@PathVariable Long templateId) {
        CouponTemplate couponTemplate = couponManageService.getCouponTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(CouponTemplateResponseDto.from(couponTemplate)));
    }

    @Operation(summary = "쿠폰 템플릿 수정")
    @PutMapping("/templates/{templateId}")
    public ResponseEntity<ApiResponse<CouponTemplateResponseDto>> updateCouponTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CouponTemplateUpdateDto updateDto) {
        CouponTemplate updatedTemplate = couponManageService.updateCouponTemplate(templateId, updateDto);
        return ResponseEntity.ok(ApiResponse.success(CouponTemplateResponseDto.from(updatedTemplate)));
    }

    @Operation(summary = "쿠폰 템플릿 삭제")
    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteCouponTemplate(@PathVariable Long templateId) {
        couponManageService.deleteCouponTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}