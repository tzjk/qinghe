package com.qinghe.life.vo;

import lombok.Data;

@Data
public class CouponStatsVO {
    private Long couponId;
    private long receivedCount;
    private long lockedCount;
    private long usedCount;
    private long expiredCount;
}
