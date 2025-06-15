package com.grepp.matnam.app.model.coupon.service;

import com.grepp.matnam.app.model.coupon.dto.CouponValidationResponseDto;
import com.grepp.matnam.app.model.coupon.entity.UserCoupon;
import com.grepp.matnam.app.model.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponValidationService {

    private final UserCouponRepository userCouponRepository;

    @Transactional
    public CouponValidationResponseDto validateAndUseCoupon(String couponCode) {
        UserCoupon userCoupon = userCouponRepository.findByCouponCode(couponCode)
                .orElse(null);

        if (userCoupon == null) {
            return new CouponValidationResponseDto(false, "존재하지 않는 쿠폰입니다.", null, null);
        }

        try {
            userCoupon.use();
            userCouponRepository.save(userCoupon);

            String discountInfo = userCoupon.getCouponTemplate().getDiscountType().name() + ": " + userCoupon.getCouponTemplate().getDiscountValue();
            return new CouponValidationResponseDto(true, "쿠폰이 정상적으로 사용되었습니다.", userCoupon.getCouponTemplate().getName(), discountInfo);

        } catch (IllegalStateException e) {
            return new CouponValidationResponseDto(false, e.getMessage(), userCoupon.getCouponTemplate().getName(), null);
        }
    }
}