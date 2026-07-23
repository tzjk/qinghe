package com.qinghe.life.vo;

import lombok.Data;

import java.util.List;

@Data
public class HomeSummaryVO {
    private List<ShopVO> banners;
    private List<CategoryVO> categories;
    private List<ShopVO> recommendedShops;
    private List<GoodsVO> hotGoods;
    private List<CouponVO> availableCoupons;
    private List<BlogVO> featuredBlogs;
}
