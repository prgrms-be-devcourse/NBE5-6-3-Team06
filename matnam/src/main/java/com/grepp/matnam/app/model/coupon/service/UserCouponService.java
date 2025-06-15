package com.grepp.matnam.app.model.coupon.service;

import com.grepp.matnam.app.model.coupon.dto.UserCouponResponseDto;
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
}