package com.grepp.matnam.app.model.coupon.service;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.entity.UserCoupon;
import com.grepp.matnam.app.model.coupon.repository.CouponTemplateRepository;
import com.grepp.matnam.app.model.coupon.repository.UserCouponRepository;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final StringRedisTemplate redisTemplate;
    private final UserCouponRepository userCouponRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final UserRepository userRepository;

    public void applyForCoupon(String userId, Long couponTemplateId) {
        if (userCouponRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        Long isNewApplicant = redisTemplate.opsForSet().add(getApplicantsKey(couponTemplateId), userId);

        if (isNewApplicant == null || isNewApplicant == 0) {
            System.out.println("이미 신청 대기열에 등록된 사용자입니다. userId: " + userId);
            return;
        }

        redisTemplate.opsForZSet().add(getCouponQueueKey(couponTemplateId), userId, System.currentTimeMillis());
        System.out.println("쿠폰 신청 완료. 대기열에 추가되었습니다. userId: " + userId);
    }

    @Transactional
    public void issueSingleCoupon(Long templateId, String userId) {
        CouponTemplate template = couponTemplateRepository.findByIdWithPessimisticLock(templateId)
                .orElseThrow(() -> new RuntimeException("쿠폰 템플릿을 찾을 수 없습니다."));

        if (template.getIssuedQuantity() >= template.getTotalQuantity() || template.getStatus() != CouponTemplateStatus.ACTIVE) {
            log.warn("쿠폰이 소진되었거나 비활성 상태입니다. templateId: {}", templateId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .couponTemplate(template)
                .build();
        userCouponRepository.save(userCoupon);

        template.increaseIssuedQuantity();
    }

    private String getApplicantsKey(Long couponTemplateId) {
        return "coupon:applicants:" + couponTemplateId;
    }

    private String getCouponQueueKey(Long couponTemplateId) {
        return "coupon:queue:" + couponTemplateId;
    }
}