package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.User;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.service.CouponSeckillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CouponSeckillPersistenceService {
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;

    public CouponSeckillPersistenceService(CouponMapper couponMapper, UserCouponMapper userCouponMapper, UserMapper userMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.userMapper = userMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persist(Long couponId, Long userId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !CouponSeckillService.SECKILL_COUPON_STATUS.equals(coupon.getCouponStatus())) {
            throw new BusinessException(409, "Invalid seckill coupon activity");
        }
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) {
            throw new BusinessException(409, "Seckill activity is disabled");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(409, "Invalid seckill user");
        }
        UserCoupon existing = userCouponMapper.selectOne(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId));
        if (existing != null) {
            return;
        }
        int decreased = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .eq(Coupon::getId, couponId).eq(Coupon::getStatus, CouponStatus.ENABLED.name())
                .gt(Coupon::getAvailableStock, 0)
                .setSql("available_stock = available_stock - 1, claimed_count = claimed_count + 1"));
        if (decreased != 1) {
            throw new BusinessException(409, "MySQL coupon stock is unavailable");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatus.AVAILABLE.name());
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpireTime(coupon.getUseEndTime());
        if (userCouponMapper.insert(userCoupon) != 1) {
            throw new BusinessException(500, "Unable to persist seckill coupon");
        }
    }
}
