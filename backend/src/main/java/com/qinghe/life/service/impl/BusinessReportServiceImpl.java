package com.qinghe.life.service.impl;

import com.qinghe.life.dto.BusinessReportQuery;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.BusinessReportMapper;
import com.qinghe.life.service.BusinessReportService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.vo.BusinessOverviewVO;
import com.qinghe.life.vo.BusinessTrendVO;
import com.qinghe.life.vo.CouponUsageSummaryVO;
import com.qinghe.life.vo.GoodsSalesRankingVO;
import com.qinghe.life.vo.ShopSalesRankingVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BusinessReportServiceImpl implements BusinessReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 90;
    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 50;
    private final BusinessReportMapper businessReportMapper;

    public BusinessReportServiceImpl(BusinessReportMapper businessReportMapper) {
        this.businessReportMapper = businessReportMapper;
    }

    @Override
    public BusinessOverviewVO overview() {
        requireAdmin();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        BusinessOverviewVO view = businessReportMapper.selectOverview(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        if (view == null) view = new BusinessOverviewVO();
        normalizeOverview(view);
        Long newUserCount = businessReportMapper.selectNewUserCount(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        view.setNewUserCount(newUserCount == null ? 0L : newUserCount);
        return view;
    }

    @Override
    public List<BusinessTrendVO> trend(BusinessReportQuery query) {
        requireAdmin();
        DateRange range = resolveRange(query);
        List<BusinessTrendVO> aggregated = businessReportMapper.selectTrend(range.startTime, range.endTime);
        Map<String, BusinessTrendVO> byDate = new HashMap<String, BusinessTrendVO>();
        for (BusinessTrendVO item : aggregated) { normalizeTrend(item); byDate.put(item.getReportDate(), item); }
        List<BusinessTrendVO> result = new ArrayList<BusinessTrendVO>();
        for (LocalDate date = range.startDate; !date.isAfter(range.endDate); date = date.plusDays(1)) {
            String key = date.toString(); BusinessTrendVO item = byDate.get(key);
            result.add(item == null ? emptyTrend(key) : item);
        }
        return result;
    }

    @Override public List<ShopSalesRankingVO> shopRanking(BusinessReportQuery query) { requireAdmin(); DateRange range = resolveRange(query); return businessReportMapper.selectShopRanking(range.startTime, range.endTime, resolveTop(query)); }
    @Override public List<GoodsSalesRankingVO> goodsRanking(BusinessReportQuery query) { requireAdmin(); DateRange range = resolveRange(query); return businessReportMapper.selectGoodsRanking(range.startTime, range.endTime, resolveTop(query)); }
    @Override public List<CouponUsageSummaryVO> couponSummary(BusinessReportQuery query) { requireAdmin(); DateRange range = resolveRange(query); return businessReportMapper.selectCouponSummary(range.startTime, range.endTime); }

    private DateRange resolveRange(BusinessReportQuery query) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate end = query == null || query.getEndDate() == null ? today : query.getEndDate();
        LocalDate start = query == null || query.getStartDate() == null ? end.minusDays(DEFAULT_DAYS - 1L) : query.getStartDate();
        if (start.isAfter(end)) throw new BusinessException(400, "开始日期不能晚于结束日期");
        if (start.plusDays(MAX_RANGE_DAYS - 1L).isBefore(end)) throw new BusinessException(400, "日期范围不能超过90天");
        return new DateRange(start, end, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private int resolveTop(BusinessReportQuery query) {
        int top = query == null || query.getTop() == null ? DEFAULT_TOP : query.getTop();
        if (top < 1 || top > MAX_TOP) throw new BusinessException(400, "排行数量必须在1到50之间");
        return top;
    }

    private void requireAdmin() { if (AdminContext.getAdminId() == null) throw new BusinessException(401, "管理员登录已过期"); }
    private void normalizeOverview(BusinessOverviewVO view) {
        view.setOrderCount(zero(view.getOrderCount())); view.setPaidOrderCount(zero(view.getPaidOrderCount())); view.setCompletedOrderCount(zero(view.getCompletedOrderCount())); view.setCancelledOrderCount(zero(view.getCancelledOrderCount())); view.setSalesAmount(zero(view.getSalesAmount())); view.setDiscountAmount(zero(view.getDiscountAmount())); view.setActiveShopCount(zero(view.getActiveShopCount())); view.setPendingPayOrderCount(zero(view.getPendingPayOrderCount())); view.setPaidPendingOrderCount(zero(view.getPaidPendingOrderCount())); view.setAcceptedOrderCount(zero(view.getAcceptedOrderCount())); view.setDeliveringOrderCount(zero(view.getDeliveringOrderCount()));
    }
    private void normalizeTrend(BusinessTrendVO view) { view.setOrderCount(zero(view.getOrderCount())); view.setPaidOrderCount(zero(view.getPaidOrderCount())); view.setCompletedOrderCount(zero(view.getCompletedOrderCount())); view.setCancelledOrderCount(zero(view.getCancelledOrderCount())); view.setSalesAmount(zero(view.getSalesAmount())); view.setDiscountAmount(zero(view.getDiscountAmount())); }
    private BusinessTrendVO emptyTrend(String date) { BusinessTrendVO view = new BusinessTrendVO(); view.setReportDate(date); normalizeTrend(view); return view; }
    private Long zero(Long value) { return value == null ? 0L : value; }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static class DateRange { private final LocalDate startDate; private final LocalDate endDate; private final LocalDateTime startTime; private final LocalDateTime endTime; private DateRange(LocalDate startDate, LocalDate endDate, LocalDateTime startTime, LocalDateTime endTime) { this.startDate = startDate; this.endDate = endDate; this.startTime = startTime; this.endTime = endTime; } }
}
