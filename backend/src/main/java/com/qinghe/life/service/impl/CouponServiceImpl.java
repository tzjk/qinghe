package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.enums.CouponClaimStatus;
import com.qinghe.life.enums.CouponType;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.service.CouponService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.CouponVO;
import com.qinghe.life.vo.CouponClaimVO;
import com.qinghe.life.vo.UserCouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CouponServiceImpl implements CouponService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public PageResult<CouponVO> pageAvailable(CouponPageQuery query) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, UserCoupon> userCouponMap = userCouponMap(UserContext.getUserId());
        List<Long> claimedCouponIds = new ArrayList<Long>(userCouponMap.keySet());
        Page<Coupon> page = couponMapper.selectPage(new Page<Coupon>(query.getPage(), query.getSize()),
                Wrappers.<Coupon>lambdaQuery().eq(Coupon::getStatus, CouponStatus.ENABLED.name())
                        .le(Coupon::getReceiveStartTime, now).ge(Coupon::getReceiveEndTime, now)
                        .and(wrapper -> {
                            wrapper.gt(Coupon::getAvailableStock, 0);
                            if (!claimedCouponIds.isEmpty()) wrapper.or().in(Coupon::getId, claimedCouponIds);
                        }).orderByDesc(Coupon::getCreateTime));
        return new PageResult<CouponVO>(couponViews(page.getRecords(), userCouponMap), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<UserCouponVO> mine(CouponPageQuery query) {
        Long userId = requireCurrentUserId();
        expireAvailableCoupons(userId, LocalDateTime.now());
        validateUserCouponStatus(query.getStatus());
        Page<UserCoupon> page = userCouponMapper.selectPage(new Page<UserCoupon>(query.getPage(), query.getSize()),
                Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, userId)
                        .eq(hasText(query.getStatus()), UserCoupon::getStatus, query.getStatus())
                        .orderByDesc(UserCoupon::getReceiveTime).orderByDesc(UserCoupon::getId));
        return new PageResult<UserCouponVO>(userCouponViews(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponClaimVO claim(Long couponId) {
        Long userId = requireCurrentUserId();
        Coupon coupon = requireCoupon(couponId);
        UserCoupon existing = userCouponMapper.selectOne(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId));
        if (existing != null) {
            return CouponClaimVO.alreadyClaimed(existing);
        }
        LocalDateTime now = LocalDateTime.now();
        CouponClaimVO unavailable = claimUnavailable(coupon, now);
        if (unavailable != null) {
            return unavailable;
        }
        int decreased = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate().eq(Coupon::getId, couponId)
                .eq(Coupon::getStatus, CouponStatus.ENABLED.name()).gt(Coupon::getAvailableStock, 0)
                .setSql("available_stock = available_stock - 1"));
        if (decreased != 1) {
            CouponClaimVO changed = claimUnavailable(requireCoupon(couponId), LocalDateTime.now());
            return changed == null ? CouponClaimVO.unavailable(CouponClaimStatus.OUT_OF_STOCK, "优惠券库存不足") : changed;
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId); userCoupon.setCouponId(couponId); userCoupon.setStatus(UserCouponStatus.AVAILABLE.name());
        userCoupon.setReceiveTime(now); userCoupon.setExpireTime(coupon.getUseEndTime());
        if (userCouponMapper.insert(userCoupon) != 1) {
            throw new BusinessException(500, "优惠券领取失败");
        }
        return CouponClaimVO.success(userCoupon);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public BigDecimal lockForOrder(Long userId, Long userCouponId, Long shopId, BigDecimal totalAmount,
                                   Long orderId, LocalDateTime now) {
        if (userCouponId == null) {
            return ZERO;
        }
        UserCoupon userCoupon = userCouponMapper.selectOne(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getId, userCouponId).eq(UserCoupon::getUserId, userId));
        if (userCoupon == null) {
            throw new BusinessException(404, "优惠券不存在或无权使用");
        }
        if (!UserCouponStatus.AVAILABLE.name().equals(userCoupon.getStatus())) {
            throw new BusinessException(409, "优惠券当前不可使用");
        }
        Coupon coupon = requireCoupon(userCoupon.getCouponId());
        validateOrderApplicable(coupon, userCoupon, shopId, totalAmount, now);
        BigDecimal discount = calculateDiscount(coupon, totalAmount);
        int updated = userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .eq(UserCoupon::getId, userCouponId).eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.name())
                .set(UserCoupon::getStatus, UserCouponStatus.LOCKED.name()).set(UserCoupon::getOrderId, orderId)
                .set(UserCoupon::getLockTime, now));
        if (updated != 1) {
            throw new BusinessException(409, "优惠券状态已变化，请刷新后重试");
        }
        return discount;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void redeemForOrder(Long orderId, LocalDateTime now) {
        List<UserCoupon> locked = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getOrderId, orderId).eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.name()));
        if (locked.isEmpty()) {
            return;
        }
        if (locked.size() != 1) {
            throw new BusinessException(409, "订单优惠券关联异常");
        }
        if (userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate().eq(UserCoupon::getId, locked.get(0).getId())
                .eq(UserCoupon::getOrderId, orderId).eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.name())
                .set(UserCoupon::getStatus, UserCouponStatus.USED.name()).set(UserCoupon::getUseTime, now)) != 1) {
            throw new BusinessException(409, "优惠券状态已变化，支付已回滚");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void releaseForCancelledOrder(Long orderId, LocalDateTime now) {
        List<UserCoupon> locked = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getOrderId, orderId).eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.name()));
        if (locked.isEmpty()) {
            return;
        }
        if (locked.size() != 1) {
            throw new BusinessException(409, "订单优惠券关联异常");
        }
        UserCoupon userCoupon = locked.get(0);
        String target = isExpired(userCoupon.getExpireTime(), now) ? UserCouponStatus.EXPIRED.name() : UserCouponStatus.AVAILABLE.name();
        if (userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate().eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getOrderId, orderId).eq(UserCoupon::getStatus, UserCouponStatus.LOCKED.name())
                .set(UserCoupon::getStatus, target).set(UserCoupon::getOrderId, null)) != 1) {
            throw new BusinessException(409, "优惠券状态已变化，取消已回滚");
        }
    }

    private void expireAvailableCoupons(Long userId, LocalDateTime now) {
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate().eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.name()).lt(UserCoupon::getExpireTime, now)
                .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.name()));
    }

    private void validateClaimable(Coupon coupon, LocalDateTime now) {
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) throw new BusinessException(409, "优惠券未启用");
        if (!between(now, coupon.getReceiveStartTime(), coupon.getReceiveEndTime())) throw new BusinessException(409, "当前不在优惠券领取时间内");
        if (coupon.getAvailableStock() == null || coupon.getAvailableStock() <= 0) throw new BusinessException(409, "优惠券库存不足");
        if (!Integer.valueOf(1).equals(coupon.getPerUserLimit())) throw new BusinessException(409, "当前数据结构仅支持每人限领一张");
    }

    private void validateOrderApplicable(Coupon coupon, UserCoupon userCoupon, Long shopId, BigDecimal totalAmount, LocalDateTime now) {
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) throw new BusinessException(409, "优惠券已停用");
        if (!between(now, coupon.getUseStartTime(), coupon.getUseEndTime()) || isExpired(userCoupon.getExpireTime(), now)) throw new BusinessException(409, "优惠券已过期或未到使用时间");
        if (!shopId.equals(coupon.getShopId())) throw new BusinessException(400, "优惠券不适用于当前店铺");
        if (totalAmount.compareTo(coupon.getThresholdAmount()) < 0) throw new BusinessException(400, "订单未达到优惠券使用门槛");
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal totalAmount) {
        BigDecimal discount;
        if (CouponType.CASH.name().equals(coupon.getCouponType())) {
            discount = coupon.getDiscountAmount();
        } else if (CouponType.DISCOUNT.name().equals(coupon.getCouponType())) {
            discount = totalAmount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        } else {
            throw new BusinessException(409, "优惠券类型无效");
        }
        if (discount == null || discount.compareTo(ZERO) < 0) throw new BusinessException(409, "优惠券优惠金额无效");
        return discount.min(totalAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon requireCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) throw new BusinessException(404, "优惠券不存在");
        return coupon;
    }

    private CouponClaimVO claimUnavailable(Coupon coupon, LocalDateTime now) {
        if (!CouponStatus.ENABLED.name().equals(coupon.getStatus())) {
            return CouponClaimVO.unavailable(CouponClaimStatus.DISABLED, "优惠券未启用");
        }
        if (coupon.getReceiveStartTime() == null || now.isBefore(coupon.getReceiveStartTime())) {
            return CouponClaimVO.unavailable(CouponClaimStatus.NOT_STARTED, "优惠券活动尚未开始");
        }
        if (coupon.getReceiveEndTime() == null || now.isAfter(coupon.getReceiveEndTime())) {
            return CouponClaimVO.unavailable(CouponClaimStatus.ENDED, "优惠券活动已结束");
        }
        if (coupon.getAvailableStock() == null || coupon.getAvailableStock() <= 0) {
            return CouponClaimVO.unavailable(CouponClaimStatus.OUT_OF_STOCK, "优惠券库存不足");
        }
        if (!Integer.valueOf(1).equals(coupon.getPerUserLimit())) {
            throw new BusinessException(409, "当前数据结构仅支持每人限领一张");
        }
        return null;
    }

    private Map<Long, UserCoupon> userCouponMap(Long userId) {
        if (userId == null) return Collections.emptyMap();
        Map<Long, UserCoupon> result = new HashMap<Long, UserCoupon>();
        for (UserCoupon userCoupon : userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId))) {
            result.put(userCoupon.getCouponId(), userCoupon);
        }
        return result;
    }

    private List<CouponVO> couponViews(List<Coupon> coupons, Map<Long, UserCoupon> userCouponMap) {
        if (coupons.isEmpty()) return Collections.emptyList();
        List<CouponVO> result = new ArrayList<CouponVO>();
        for (Coupon coupon : coupons) {
            CouponVO view = CouponVO.from(coupon);
            UserCoupon userCoupon = userCouponMap.get(coupon.getId());
            if (userCoupon != null) view.markClaimed(userCoupon.getId(), userCoupon.getStatus());
            result.add(view);
        }
        return result;
    }

    private List<UserCouponVO> userCouponViews(List<UserCoupon> userCoupons) {
        if (userCoupons.isEmpty()) return Collections.emptyList();
        List<Long> couponIds = new ArrayList<Long>();
        for (UserCoupon userCoupon : userCoupons) couponIds.add(userCoupon.getCouponId());
        Map<Long, Coupon> couponMap = new HashMap<Long, Coupon>();
        for (Coupon coupon : couponMapper.selectBatchIds(couponIds)) couponMap.put(coupon.getId(), coupon);
        List<UserCouponVO> result = new ArrayList<UserCouponVO>();
        for (UserCoupon userCoupon : userCoupons) result.add(UserCouponVO.from(userCoupon, couponMap.get(userCoupon.getCouponId())));
        return result;
    }

    private void validateUserCouponStatus(String status) {
        if (!hasText(status)) return;
        try { UserCouponStatus.valueOf(status); } catch (IllegalArgumentException exception) { throw new BusinessException(400, "优惠券状态无效"); }
    }
    private boolean between(LocalDateTime now, LocalDateTime start, LocalDateTime end) { return start != null && end != null && !now.isBefore(start) && !now.isAfter(end); }
    private boolean isExpired(LocalDateTime expireTime, LocalDateTime now) { return expireTime == null || now.isAfter(expireTime); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private Long requireCurrentUserId() { Long userId = UserContext.getUserId(); if (userId == null) throw new BusinessException(401, "未登录或登录已过期"); return userId; }
}
