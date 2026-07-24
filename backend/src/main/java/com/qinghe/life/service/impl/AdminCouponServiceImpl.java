package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminCouponSaveRequest;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.UserCoupon;
import com.qinghe.life.enums.CouponStatus;
import com.qinghe.life.enums.CouponType;
import com.qinghe.life.enums.UserCouponStatus;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserCouponMapper;
import com.qinghe.life.service.AdminCouponService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.vo.CouponStatsVO;
import com.qinghe.life.vo.CouponVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminCouponServiceImpl implements AdminCouponService {
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ShopMapper shopMapper;

    public AdminCouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper, ShopMapper shopMapper) {
        this.couponMapper = couponMapper; this.userCouponMapper = userCouponMapper; this.shopMapper = shopMapper;
    }
    @Override public PageResult<CouponVO> page(CouponPageQuery query) {
        requireAdmin();
        Page<Coupon> page = couponMapper.selectPage(new Page<Coupon>(query.getPage(), query.getSize()), Wrappers.<Coupon>lambdaQuery()
                .eq(hasText(query.getStatus()), Coupon::getStatus, query.getStatus()).orderByDesc(Coupon::getCreateTime));
        List<CouponVO> views = new ArrayList<CouponVO>(); for (Coupon coupon : page.getRecords()) views.add(CouponVO.from(coupon));
        return new PageResult<CouponVO>(views, page.getTotal(), page.getCurrent(), page.getSize());
    }
    @Override @Transactional(rollbackFor = Exception.class) public CouponVO create(AdminCouponSaveRequest request) {
        requireAdmin(); Coupon coupon = buildAndValidate(request); coupon.setAvailableStock(coupon.getTotalStock());
        if (couponMapper.insert(coupon) != 1) throw new BusinessException(500, "创建优惠券失败"); return CouponVO.from(coupon);
    }
    @Override @Transactional(rollbackFor = Exception.class) public CouponVO update(Long couponId, AdminCouponSaveRequest request) {
        requireAdmin(); Coupon existing = requireCoupon(couponId);
        if (!LocalDateTime.now().isBefore(existing.getReceiveStartTime())) throw new BusinessException(409, "领取开始后不能修改优惠券规则");
        Coupon updated = buildAndValidate(request); updated.setId(couponId); updated.setAvailableStock(updated.getTotalStock());
        if (couponMapper.updateById(updated) != 1) throw new BusinessException(409, "优惠券状态已变化，请刷新后重试"); return CouponVO.from(updated);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void changeStatus(Long couponId, String status) {
        requireAdmin(); if (!CouponStatus.isValid(status)) throw new BusinessException(400, "优惠券状态无效"); requireCoupon(couponId);
        if (couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate().eq(Coupon::getId, couponId).set(Coupon::getStatus, status)) != 1) throw new BusinessException(409, "优惠券状态已变化");
    }
    @Override public CouponStatsVO stats(Long couponId) {
        requireAdmin(); requireCoupon(couponId); CouponStatsVO view = new CouponStatsVO(); view.setCouponId(couponId);
        view.setReceivedCount(userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, couponId)));
        view.setLockedCount(count(couponId, UserCouponStatus.LOCKED)); view.setUsedCount(count(couponId, UserCouponStatus.USED)); view.setExpiredCount(count(couponId, UserCouponStatus.EXPIRED)); return view;
    }
    private long count(Long couponId, UserCouponStatus status) { return userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getCouponId, couponId).eq(UserCoupon::getStatus, status.name())); }
    private Coupon buildAndValidate(AdminCouponSaveRequest request) {
        if (!request.getReceiveStartTime().isBefore(request.getReceiveEndTime()) || !request.getUseStartTime().isBefore(request.getUseEndTime())) throw new BusinessException(400, "优惠券时间范围无效");
        if (!Integer.valueOf(1).equals(request.getPerUserLimit())) throw new BusinessException(400, "当前数据结构仅支持每人限领一张");
        Shop shop = shopMapper.selectById(request.getShopId()); if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) throw new BusinessException(400, "适用店铺不存在或未营业");
        if (CouponType.CASH.name().equals(request.getCouponType())) { if (request.getDiscountAmount() == null || request.getDiscountRate() != null) throw new BusinessException(400, "固定金额券必须填写优惠金额且不填写折扣率"); }
        else if (CouponType.DISCOUNT.name().equals(request.getCouponType())) { if (request.getDiscountRate() == null || request.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || request.getDiscountRate().compareTo(BigDecimal.ONE) >= 0 || request.getDiscountAmount() != null) throw new BusinessException(400, "折扣券必须填写0到1之间的折扣率"); }
        else throw new BusinessException(400, "优惠券类型无效");
        Coupon coupon = new Coupon(); coupon.setName(request.getName().trim()); coupon.setCouponType(request.getCouponType()); coupon.setDiscountAmount(request.getDiscountAmount()); coupon.setDiscountRate(request.getDiscountRate()); coupon.setThresholdAmount(request.getThresholdAmount()); coupon.setTotalStock(request.getTotalStock()); coupon.setClaimedCount(0); coupon.setCouponStatus(CouponStatus.ENABLED.name().equals(request.getStatus()) ? "PUBLISHED" : "DRAFT"); coupon.setStartTime(request.getReceiveStartTime()); coupon.setEndTime(request.getUseEndTime()); coupon.setReceiveStartTime(request.getReceiveStartTime()); coupon.setReceiveEndTime(request.getReceiveEndTime()); coupon.setUseStartTime(request.getUseStartTime()); coupon.setUseEndTime(request.getUseEndTime()); coupon.setShopId(request.getShopId()); coupon.setPerUserLimit(request.getPerUserLimit()); coupon.setStatus(request.getStatus()); return coupon;
    }
    private Coupon requireCoupon(Long couponId) { Coupon coupon = couponMapper.selectById(couponId); if (coupon == null) throw new BusinessException(404, "优惠券不存在"); return coupon; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private Long requireAdmin() { Long adminId = AdminContext.getAdminId(); if (adminId == null) throw new BusinessException(401, "管理员登录已过期"); return adminId; }
}
