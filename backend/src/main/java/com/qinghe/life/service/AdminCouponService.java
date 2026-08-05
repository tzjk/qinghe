package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminCouponSaveRequest;
import com.qinghe.life.dto.CouponPageQuery;
import com.qinghe.life.vo.CouponStatsVO;
import com.qinghe.life.vo.CouponVO;

public interface AdminCouponService {
    PageResult<CouponVO> page(CouponPageQuery query);

    CouponVO create(AdminCouponSaveRequest request);

    CouponVO update(Long couponId, AdminCouponSaveRequest request);

    void changeStatus(Long couponId, String status);

    CouponStatsVO stats(Long couponId);
}
