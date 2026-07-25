package com.qinghe.life.service;

import com.qinghe.life.dto.BusinessReportQuery;
import com.qinghe.life.vo.BusinessOverviewVO;
import com.qinghe.life.vo.BusinessTrendVO;
import com.qinghe.life.vo.CouponUsageSummaryVO;
import com.qinghe.life.vo.GoodsSalesRankingVO;
import com.qinghe.life.vo.ShopSalesRankingVO;

import java.util.List;

public interface BusinessReportService {
    BusinessOverviewVO overview();
    List<BusinessTrendVO> trend(BusinessReportQuery query);
    List<ShopSalesRankingVO> shopRanking(BusinessReportQuery query);
    List<GoodsSalesRankingVO> goodsRanking(BusinessReportQuery query);
    List<CouponUsageSummaryVO> couponSummary(BusinessReportQuery query);
}
