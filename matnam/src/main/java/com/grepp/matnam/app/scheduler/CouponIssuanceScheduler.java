package com.grepp.matnam.app.scheduler;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.repository.CouponTemplateRepository;
import com.grepp.matnam.app.model.coupon.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssuanceScheduler {

    private final StringRedisTemplate redisTemplate;
    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponIssueService couponIssueService;

    private static final String ACTIVE_COUPON_TEMPLATES_KEY = "coupon:active_templates";
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 1000)
    public void issueCouponsFromQueue() {
        Set<String> activeTemplatesIds = redisTemplate.opsForSet().members(ACTIVE_COUPON_TEMPLATES_KEY);
        if (activeTemplatesIds == null || activeTemplatesIds.isEmpty()) {
            return;
        }

        for (String templateIdStr : activeTemplatesIds) {
            Long templateId = Long.parseLong(templateIdStr);
            CouponTemplate template = couponTemplateRepository.findById(templateId).orElse(null);

            if (template == null || (template.getStatus() != CouponTemplateStatus.ACTIVE && template.getStatus() != CouponTemplateStatus.EXHAUSTED) || template.getEndAt().isBefore(LocalDateTime.now())) {
                redisTemplate.opsForSet().remove(ACTIVE_COUPON_TEMPLATES_KEY, templateIdStr);
                continue;
            }

            if (template.getStatus() == CouponTemplateStatus.EXHAUSTED || template.getIssuedQuantity() >= template.getTotalQuantity()) {
                log.warn("쿠폰 소진으로 대기열 정리 시작. templateId: {}", templateId);

                Long remainingCount = redisTemplate.opsForZSet().zCard(getCouponQueueKey(templateId));
                log.info("정리할 대기열 인원: {}명", remainingCount);

                redisTemplate.delete(getCouponQueueKey(templateId));
                redisTemplate.delete(getApplicantsKey(templateId));
                redisTemplate.opsForSet().remove(ACTIVE_COUPON_TEMPLATES_KEY, templateIdStr);

                log.info("대기열 정리 완료. templateId: {}, 정리된 인원: {}명", templateId, remainingCount);
                continue;
            }

            Set<String> userIds = redisTemplate.opsForZSet().range(getCouponQueueKey(templateId), 0, BATCH_SIZE - 1);
            if (userIds == null || userIds.isEmpty()) {
                continue;
            }

            for (String userId : userIds) {
                try {
                    couponIssueService.issueSingleCoupon(Long.parseLong(templateIdStr), userId);
                } catch (Exception e) {
                    log.error("쿠폰 발급 중 오류 발생. templateId: {}, userId: {}", templateId, userId, e);
                } finally {
                    redisTemplate.opsForZSet().remove(getCouponQueueKey(templateId), userId);
                }
            }
        }
    }

    private String getCouponQueueKey(Long couponTemplateId) {
        return "coupon:queue:" + couponTemplateId;
    }

    private String getApplicantsKey(Long couponTemplateId) {
        return "coupon:applicants:" + couponTemplateId;
    }
}