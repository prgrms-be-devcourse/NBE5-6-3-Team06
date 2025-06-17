package com.grepp.matnam.app.model.coupon.service;

import com.grepp.matnam.app.facade.NotificationSender;
import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.dto.CouponTemplateCreateDto;
import com.grepp.matnam.app.model.coupon.dto.CouponTemplateUpdateDto;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.repository.CouponTemplateRepository;
import com.grepp.matnam.app.model.notification.code.NotificationType;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.restaurant.repository.RestaurantRepository;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.grepp.matnam.app.model.team.entity.QTeam.team;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponManageService {

    private final CouponTemplateRepository couponTemplateRepository;
    private final RestaurantRepository restaurantRepository;
    private final StringRedisTemplate redisTemplate;
    private final NotificationSender notificationSender;

    private static final String ACTIVE_COUPON_TEMPLATES_KEY = "coupon:active_templates";
    private final UserService userService;

    @Transactional
    public CouponTemplate createCouponTemplate(CouponTemplateCreateDto dto) {
        Restaurant restaurant = restaurantRepository.findById(dto.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("해당 음식점을 찾을 수 없습니다. id: " + dto.getRestaurantId()));

        CouponTemplate couponTemplate = CouponTemplate.builder()
                .restaurant(restaurant)
                .name(dto.getName())
                .description(dto.getDescription())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .totalQuantity(dto.getTotalQuantity())
                .issuedQuantity(0)
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .validDays(dto.getValidDays())
                .status(CouponTemplateStatus.ACTIVE)
                .build();

        CouponTemplate savedTemplate = couponTemplateRepository.save(couponTemplate);

        redisTemplate.opsForSet().add(ACTIVE_COUPON_TEMPLATES_KEY, String.valueOf(savedTemplate.getTemplateId()));

        List<User> activatedUsers = userService.findAllUsers();

        for (User user : activatedUsers) {
            notificationSender.sendNotificationToUser(user.getUserId(),
                    NotificationType.COUPON_ISSUED, "맛있는 알림이 도착했습니다 !\n[" +
                            couponTemplate.getRestaurant().getName() +
                            "] " + "할인 쿠폰 발급 예정 알림 (" + couponTemplate.getTotalQuantity() + "명)",
                    "/coupons");
        }

        return savedTemplate;
    }

    public CouponTemplate getCouponTemplate(Long templateId) {
        return couponTemplateRepository.findByIdWithRestaurant(templateId)
                .orElseThrow(() -> new EntityNotFoundException("해당 쿠폰 템플릿을 찾을 수 없습니다. id: " + templateId));
    }

    @Transactional
    public CouponTemplate updateCouponTemplate(Long templateId, CouponTemplateUpdateDto dto) {
        CouponTemplate template = couponTemplateRepository.findByIdWithRestaurant(templateId)
                .orElseThrow(() -> new EntityNotFoundException("해당 쿠폰 템플릿을 찾을 수 없습니다. id: " + templateId));

        if (dto.getName() != null) {
            template.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            template.setDescription(dto.getDescription());
        }

        if (dto.getDiscountType() != null) {
            template.setDiscountType(dto.getDiscountType());
        }
        if (dto.getDiscountValue() != null) {
            template.setDiscountValue(dto.getDiscountValue());
        }

        if (dto.getTotalQuantity() != null) {
            if (dto.getTotalQuantity() < template.getIssuedQuantity()) {
                throw new IllegalArgumentException("총 발급 수량은 이미 발급된 수량(" + template.getIssuedQuantity() + ")보다 적을 수 없습니다.");
            }
            template.setTotalQuantity(dto.getTotalQuantity());
        }
        if (dto.getValidDays() != null) {
            template.setValidDays(dto.getValidDays());
        }

        if (dto.getEndAt() != null && dto.getEndAt().isAfter(LocalDateTime.now()) && dto.getEndAt().isBefore(template.getEndAt())) {
            template.setEndAt(dto.getEndAt());
        }

        return template;
    }

    @Transactional
    public void deleteCouponTemplate(Long templateId) {
        CouponTemplate template = couponTemplateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("해당 쿠폰 템플릿을 찾을 수 없습니다. id: " + templateId));

        template.setStatus(CouponTemplateStatus.DELETED);

        redisTemplate.opsForSet().remove(ACTIVE_COUPON_TEMPLATES_KEY, String.valueOf(templateId));
    }

    public Page<CouponTemplate> getCouponTemplates(Pageable pageable) {
        return couponTemplateRepository.findAll(pageable);
    }

    public Page<CouponTemplate> findByFilter(String status, String keyword, Pageable pageable) {
        CouponTemplateStatus couponStatus = null;
        if (!status.isEmpty()) {
            try {
                couponStatus = CouponTemplateStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // 잘못된 상태값인 경우 null
            }
        }

        return couponTemplateRepository.findByFilter(couponStatus, keyword, pageable);
    }

    public Page<CouponTemplate> findAvailableCoupons(String keyword, Pageable pageable) {
        return couponTemplateRepository.findAvailableCoupons(keyword, pageable);
    }

    public Page<CouponTemplate> findAllActiveCoupons(String keyword, Pageable pageable) {
        return couponTemplateRepository.findAllActiveCoupons(keyword, pageable);
    }

    public Page<CouponTemplate> findAllActiveCouponsWithDefaultSort(String keyword, Pageable pageable) {
        return couponTemplateRepository.findAllActiveCouponsWithDefaultSort(keyword, pageable);
    }
}