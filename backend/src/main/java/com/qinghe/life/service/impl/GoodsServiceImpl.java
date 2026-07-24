package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.cache.CachedGoods;
import com.qinghe.life.cache.CatalogCache;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.config.CatalogCacheProperties;
import com.qinghe.life.dto.GoodsQuery;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.service.GoodsService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.GoodsVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl implements GoodsService {
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final CatalogCache catalogCache;
    private final CatalogCacheProperties cacheProperties;

    public GoodsServiceImpl(GoodsMapper goodsMapper, ShopMapper shopMapper, CatalogCache catalogCache,
                            CatalogCacheProperties cacheProperties) {
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
        this.catalogCache = catalogCache;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public PageResult<GoodsVO> page(GoodsQuery query) {
        List<Long> enabledShopIds = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().eq(Shop::getStatus, 1))
                .stream().map(Shop::getId).collect(Collectors.toList());
        if (enabledShopIds.isEmpty()) {
            return new PageResult<GoodsVO>(java.util.Collections.<GoodsVO>emptyList(), 0L, query.getPage(), query.getSize());
        }
        Page<Goods> page = goodsMapper.selectPage(new Page<Goods>(query.getPage(), query.getSize()),
                Wrappers.<Goods>lambdaQuery().eq(Goods::getSaleStatus, "ON_SALE")
                        .eq(query.getShopId() != null, Goods::getShopId, query.getShopId())
                        .in(Goods::getShopId, enabledShopIds)
                        .orderByDesc(Goods::getSalesCount).orderByDesc(Goods::getId));
        return new PageResult<GoodsVO>(page.getRecords().stream().map(GoodsVO::fromGoods).collect(Collectors.toList()),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public GoodsVO detail(Long id) {
        CachedGoods cached = catalogCache.getObject(RedisKeys.goodsDetail(id), RedisKeys.goodsLock(id), CachedGoods.class,
                cacheProperties.getGoodsTtlMinutes(), () -> loadCachedGoods(id));
        if (cached == null) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        Goods current = requireSaleableGoods(id);
        return cached.toGoodsVO(current);
    }

    private CachedGoods loadCachedGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null || !"ON_SALE".equals(goods.getSaleStatus())) {
            return null;
        }
        Shop shop = shopMapper.selectById(goods.getShopId());
        return shop == null || !Integer.valueOf(1).equals(shop.getStatus()) ? null : CachedGoods.from(goods, null);
    }

    private Goods requireSaleableGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null || !"ON_SALE".equals(goods.getSaleStatus())) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        Shop shop = shopMapper.selectById(goods.getShopId());
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        return goods;
    }
}
