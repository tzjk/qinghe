package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Update("UPDATE qh_coupon SET available_stock = available_stock - 1, claimed_count = claimed_count + 1 "
            + "WHERE id = #{couponId} AND status = 'ENABLED' AND available_stock > 0")
    int decreaseSeckillStock(@Param("couponId") Long couponId);
}
