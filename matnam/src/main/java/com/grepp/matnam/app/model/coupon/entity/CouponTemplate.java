package com.grepp.matnam.app.model.coupon.entity;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.code.DiscountType;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "coupon_templates")
public class CouponTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurantId", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private int discountValue;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private int validDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTemplateStatus status;

    public void increaseIssuedQuantity() {
        if (this.issuedQuantity >= this.totalQuantity) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
        }
        this.issuedQuantity++;
        if (this.issuedQuantity == this.totalQuantity) {
            this.status = CouponTemplateStatus.EXHAUSTED;
        }
    }
}