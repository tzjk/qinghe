package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.SeckillCouponOrder;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.SeckillCouponOrderMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.service.CouponSeckillService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transaction boundary intentionally lives in a separate Spring bean. */
@Service
public class CouponSeckillPersistenceService {
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final SeckillCouponOrderMapper seckillOrderMapper;

    public CouponSeckillPersistenceService(CouponMapper couponMapper, UserCouponMapper userCouponMapper,
            SeckillCouponOrderMapper seckillOrderMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.seckillOrderMapper = seckillOrderMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persist(Long orderId, Long couponId, Long userId) {
        SeckillCouponOrder recorded = seckillOrderMapper.selectById(orderId);
        if (recorded != null) return;
        SeckillCouponOrder sameUserCoupon = seckillOrderMapper.selectOne(Wrappers.<SeckillCouponOrder>lambdaQuery()
                .eq(SeckillCouponOrder::getUserId, userId).eq(SeckillCouponOrder::getCouponId, couponId));
        if (sameUserCoupon != null) {
            if (orderId.equals(sameUserCoupon.getId())) return;
            throw new BusinessException(409, "用户已有秒杀订单");
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !CouponSeckillService.SECKILL_COUPON_STATUS.equals(coupon.getCouponStatus())
                || !CouponStatus.ENABLED.name().equals(coupon.getStatus())) {
            throw new BusinessException(409, "秒杀活动不可用");
        }
        int decreased = couponMapper.decreaseSeckillStock(couponId);
        if (decreased != 1) throw new BusinessException(409, "数据库优惠券库存不足");
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatus.AVAILABLE.name());
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpireTime(coupon.getUseEndTime());
        if (userCouponMapper.insert(userCoupon) != 1) throw new BusinessException(500, "秒杀优惠券发放失败");
        SeckillCouponOrder order = new SeckillCouponOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setCouponId(couponId);
        order.setStatus("SUCCESS");
        if (seckillOrderMapper.insert(order) != 1) throw new BusinessException(500, "秒杀订单创建失败");
    }

    public boolean exists(Long orderId) { return seckillOrderMapper.selectById(orderId) != null; }
}
