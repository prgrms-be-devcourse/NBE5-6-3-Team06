package com.grepp.matnam.app.model.coupon.service;

import com.grepp.matnam.app.controller.web.user.payload.CouponStatsDto;
import com.grepp.matnam.app.model.coupon.code.CouponStatus;
import com.grepp.matnam.app.model.coupon.dto.UserCouponResponseDto;
import com.grepp.matnam.app.model.coupon.entity.UserCoupon;
import com.grepp.matnam.app.model.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;

    public List<UserCouponResponseDto> getMyCoupons(String userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
                .map(UserCouponResponseDto::from)
                .collect(Collectors.toList());
    }

    public CouponStatsDto getCouponStats(String userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findAllByUserId(userId);

        int totalCount = userCoupons.size();
        int availableCount = (int) userCoupons.stream().filter(c -> c.getStatus() == CouponStatus.ISSUED).count();
        int usedCount = (int) userCoupons.stream().filter(c -> c.getStatus() == CouponStatus.USED).count();
        int expiredCount = (int) userCoupons.stream().filter(c -> c.getStatus() == CouponStatus.EXPIRED).count();

        return new CouponStatsDto(totalCount, availableCount, usedCount, expiredCount);
    }
}