package com.grepp.matnam.app.controller.web.user.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponStatsDto {
    private int totalCount;      // 총 쿠폰 수
    private int availableCount;  // 사용 가능한 쿠폰 수
    private int usedCount;       // 사용 완료된 쿠폰 수
    private int expiredCount;    // 만료된 쿠폰 수
}