package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Blog;
import com.qinghe.life.entity.Coupon;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.mapper.BlogMapper;
import com.qinghe.life.mapper.CouponMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.service.CategoryService;
import com.qinghe.life.service.HomeService;
import com.qinghe.life.vo.BlogVO;
import com.qinghe.life.vo.CouponVO;
import com.qinghe.life.vo.GoodsVO;
import com.qinghe.life.vo.HomeSummaryVO;
import com.qinghe.life.vo.ShopVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeServiceImpl implements HomeService {
    private final CategoryService categoryService;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final CouponMapper couponMapper;
    private final BlogMapper blogMapper;

    public HomeServiceImpl(CategoryService categoryService, ShopMapper shopMapper, GoodsMapper goodsMapper,
                           CouponMapper couponMapper, BlogMapper blogMapper) {
        this.categoryService = categoryService;
        this.shopMapper = shopMapper;
        this.goodsMapper = goodsMapper;
        this.couponMapper = couponMapper;
        this.blogMapper = blogMapper;
    }

    @Override
    public HomeSummaryVO summary() {
        HomeSummaryVO summary = new HomeSummaryVO();
        List<ShopVO> featuredShops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                        .eq(Shop::getStatus, 1)
                        .eq(Shop::getIsFeatured, 1)
                        .orderByAsc(Shop::getSortOrder)
                        .last("LIMIT 6"))
                .stream().map(ShopVO::fromShop).collect(Collectors.toList());
        summary.setBanners(featuredShops);
        summary.setCategories(categoryService.listEnabled());
        summary.setRecommendedShops(featuredShops);
        summary.setHotGoods(goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                        .eq(Goods::getSaleStatus, "ON_SALE")
                        .orderByDesc(Goods::getSalesCount)
                        .last("LIMIT 8"))
                .stream().map(GoodsVO::fromGoods).collect(Collectors.toList()));
        LocalDateTime now = LocalDateTime.now();
        summary.setAvailableCoupons(couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getStatus, "ENABLED")
                        .le(Coupon::getReceiveStartTime, now)
                        .ge(Coupon::getReceiveEndTime, now)
                        .gt(Coupon::getAvailableStock, 0)
                        .last("LIMIT 6"))
                .stream().map(CouponVO::from).collect(Collectors.toList()));
        summary.setFeaturedBlogs(blogMapper.selectList(Wrappers.<Blog>lambdaQuery()
                        .eq(Blog::getBlogStatus, "PUBLISHED")
                        .orderByDesc(Blog::getLikeCount)
                        .orderByDesc(Blog::getFavoriteCount)
                        .last("LIMIT 6"))
                .stream().map(BlogVO::fromBlog).collect(Collectors.toList()));
        return summary;
    }
}
