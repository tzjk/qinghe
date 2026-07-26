package com.qinghe.life.mapper;

import com.qinghe.life.vo.BusinessOverviewVO;
import com.qinghe.life.vo.BusinessTrendVO;
import com.qinghe.life.vo.CouponUsageSummaryVO;
import com.qinghe.life.vo.GoodsSalesRankingVO;
import com.qinghe.life.vo.ShopSalesRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BusinessReportMapper {
    @Select("SELECT COUNT(*) AS order_count, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN 1 ELSE 0 END),0) AS paid_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),0) AS completed_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END),0) AS cancelled_order_count, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN pay_amount ELSE 0 END),0) AS sales_amount, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN total_amount - pay_amount ELSE 0 END),0) AS discount_amount, "
            + "COUNT(DISTINCT CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN shop_id END) AS active_shop_count, "
            + "COALESCE(SUM(CASE WHEN status = 'PENDING_PAY' THEN 1 ELSE 0 END),0) AS pending_pay_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END),0) AS paid_pending_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END),0) AS accepted_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'DELIVERING' THEN 1 ELSE 0 END),0) AS delivering_order_count "
            + "FROM qh_order WHERE create_time >= #{startTime} AND create_time < #{endTime}")
    BusinessOverviewVO selectOverview(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM qh_user WHERE create_time >= #{startTime} AND create_time < #{endTime}")
    Long selectNewUserCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS report_date, COUNT(*) AS order_count, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN 1 ELSE 0 END),0) AS paid_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),0) AS completed_order_count, "
            + "COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END),0) AS cancelled_order_count, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN pay_amount ELSE 0 END),0) AS sales_amount, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') THEN total_amount - pay_amount ELSE 0 END),0) AS discount_amount "
            + "FROM qh_order WHERE create_time >= #{startTime} AND create_time < #{endTime} GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY report_date")
    List<BusinessTrendVO> selectTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT s.id AS shop_id, s.name AS shop_name, COUNT(o.id) AS paid_order_count, COALESCE(SUM(o.pay_amount),0) AS sales_amount "
            + "FROM qh_order o INNER JOIN qh_shop s ON s.id = o.shop_id "
            + "WHERE o.create_time >= #{startTime} AND o.create_time < #{endTime} AND o.status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') "
            + "GROUP BY s.id, s.name ORDER BY sales_amount DESC, s.id ASC LIMIT #{limit}")
    List<ShopSalesRankingVO> selectShopRanking(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("limit") int limit);

    @Select("SELECT oi.goods_id AS goods_id, MIN(oi.goods_name) AS goods_name, COALESCE(SUM(oi.quantity),0) AS sales_quantity, COALESCE(SUM(oi.subtotal),0) AS sales_amount "
            + "FROM qh_order o INNER JOIN qh_order_item oi ON oi.order_id = o.id "
            + "WHERE o.create_time >= #{startTime} AND o.create_time < #{endTime} AND o.status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') "
            + "GROUP BY oi.goods_id ORDER BY sales_amount DESC, oi.goods_id ASC LIMIT #{limit}")
    List<GoodsSalesRankingVO> selectGoodsRanking(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("limit") int limit);

    @Select("SELECT c.id AS coupon_id, c.name AS coupon_name, COUNT(uc.id) AS used_count, COALESCE(SUM(o.total_amount - o.pay_amount),0) AS discount_amount "
            + "FROM qh_user_coupon uc INNER JOIN qh_coupon c ON c.id = uc.coupon_id INNER JOIN qh_order o ON o.id = uc.order_id "
            + "WHERE o.create_time >= #{startTime} AND o.create_time < #{endTime} AND uc.status = 'USED' AND o.status IN ('PAID','ACCEPTED','DELIVERING','COMPLETED') "
            + "GROUP BY c.id, c.name ORDER BY used_count DESC, c.id ASC")
    List<CouponUsageSummaryVO> selectCouponSummary(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
