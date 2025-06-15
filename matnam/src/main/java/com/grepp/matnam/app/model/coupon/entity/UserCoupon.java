package com.grepp.matnam.app.model.coupon.entity;

import com.grepp.matnam.app.model.coupon.code.CouponStatus;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.infra.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_coupons",
        indexes = @Index(name = "idx_coupon_code", columnList = "couponCode", unique = true))
public class UserCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateId", nullable = false)
    private CouponTemplate couponTemplate;

    @Column(nullable = false, unique = true)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public UserCoupon(User user, CouponTemplate couponTemplate) {
        this.user = user;
        this.couponTemplate = couponTemplate;
        this.couponCode = UUID.randomUUID().toString();
        this.status = CouponStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = this.issuedAt.plusDays(couponTemplate.getValidDays());
    }

    public void use() {
        if (this.status != CouponStatus.ISSUED) {
            throw new IllegalStateException("이미 사용되었거나 만료된 쿠폰입니다.");
        }
        if (LocalDateTime.now().isAfter(this.expiresAt)) {
            this.status = CouponStatus.EXPIRED;
            throw new IllegalStateException("유효 기간이 만료된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }
}