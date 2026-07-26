package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.dto.BusinessReportQuery;
import com.qinghe.life.service.BusinessReportService;
import com.qinghe.life.vo.BusinessOverviewVO;
import com.qinghe.life.vo.BusinessTrendVO;
import com.qinghe.life.vo.CouponUsageSummaryVO;
import com.qinghe.life.vo.GoodsSalesRankingVO;
import com.qinghe.life.vo.ShopSalesRankingVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminBusinessReportController {
    private final BusinessReportService businessReportService;
    public AdminBusinessReportController(BusinessReportService businessReportService) { this.businessReportService = businessReportService; }
    @GetMapping("/overview") public Result<BusinessOverviewVO> overview() { return Result.success(businessReportService.overview()); }
    @GetMapping("/trend") public Result<List<BusinessTrendVO>> trend(BusinessReportQuery query) { return Result.success(businessReportService.trend(query)); }
    @GetMapping("/shop-ranking") public Result<List<ShopSalesRankingVO>> shopRanking(BusinessReportQuery query) { return Result.success(businessReportService.shopRanking(query)); }
    @GetMapping("/goods-ranking") public Result<List<GoodsSalesRankingVO>> goodsRanking(BusinessReportQuery query) { return Result.success(businessReportService.goodsRanking(query)); }
    @GetMapping("/coupon-summary") public Result<List<CouponUsageSummaryVO>> couponSummary(BusinessReportQuery query) { return Result.success(businessReportService.couponSummary(query)); }
}
