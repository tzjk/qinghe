package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.dto.AdminGoodsCategoryQuery;
import com.qinghe.life.dto.GoodsCategorySaveRequest;
import com.qinghe.life.dto.GoodsCategoryStatusRequest;
import com.qinghe.life.dto.GoodsCategoryUpdateRequest;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.GoodsCategory;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.GoodsCategoryMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.service.AdminGoodsCategoryService;
import com.qinghe.life.vo.GoodsCategoryVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminGoodsCategoryServiceImpl implements AdminGoodsCategoryService {
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;

    public AdminGoodsCategoryServiceImpl(GoodsCategoryMapper goodsCategoryMapper, GoodsMapper goodsMapper,
                                         ShopMapper shopMapper) {
        this.goodsCategoryMapper = goodsCategoryMapper;
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    public List<GoodsCategoryVO> list(AdminGoodsCategoryQuery query) {
        requireEnabledShop(query.getShopId());
        List<GoodsCategoryVO> result = new ArrayList<GoodsCategoryVO>();
        for (GoodsCategory category : goodsCategoryMapper.selectList(Wrappers.<GoodsCategory>lambdaQuery()
                .eq(GoodsCategory::getShopId, query.getShopId())
                .eq(query.getStatus() != null, GoodsCategory::getStatus, query.getStatus())
                .orderByAsc(GoodsCategory::getSortOrder).orderByAsc(GoodsCategory::getId))) {
            result.add(toVO(category));
        }
        return result;
    }

    @Override
    public GoodsCategoryVO create(GoodsCategorySaveRequest request) {
        requireEnabledShop(request.getShopId());
        String name = normalizedName(request.getName());
        ensureUniqueName(request.getShopId(), name, null);
        GoodsCategory category = new GoodsCategory();
        category.setShopId(request.getShopId());
        category.setName(name);
        category.setSortOrder(request.getSortOrder());
        category.setStatus(1);
        if (goodsCategoryMapper.insert(category) != 1) {
            throw new BusinessException("商品分类保存失败，请稍后重试");
        }
        return toVO(category);
    }

    @Override
    public GoodsCategoryVO update(Long id, GoodsCategoryUpdateRequest request) {
        GoodsCategory category = requireCategory(id);
        String name = normalizedName(request.getName());
        ensureUniqueName(category.getShopId(), name, id);
        category.setName(name);
        category.setSortOrder(request.getSortOrder());
        if (goodsCategoryMapper.updateById(category) != 1) {
            throw new BusinessException("商品分类保存失败，请稍后重试");
        }
        return toVO(category);
    }

    @Override
    public void updateStatus(Long id, GoodsCategoryStatusRequest request) {
        GoodsCategory category = requireCategory(id);
        category.setStatus(request.statusValue());
        if (goodsCategoryMapper.updateById(category) != 1) {
            throw new BusinessException("商品分类状态保存失败，请稍后重试");
        }
    }

    private GoodsCategoryVO toVO(GoodsCategory category) {
        Long count = goodsMapper.selectCount(Wrappers.<Goods>lambdaQuery().eq(Goods::getCategoryId, category.getId()));
        return GoodsCategoryVO.from(category, count == null ? 0L : count);
    }

    private void ensureUniqueName(Long shopId, String name, Long excludedId) {
        Long count = goodsCategoryMapper.selectCount(Wrappers.<GoodsCategory>lambdaQuery()
                .eq(GoodsCategory::getShopId, shopId).eq(GoodsCategory::getName, name)
                .ne(excludedId != null, GoodsCategory::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("该店铺已存在同名商品分类");
        }
    }

    private GoodsCategory requireCategory(Long id) {
        GoodsCategory category = goodsCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "商品分类不存在");
        }
        return category;
    }

    private Shop requireEnabledShop(Long shopId) {
        Shop shop = shopId == null ? null : shopMapper.selectById(shopId);
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BusinessException("所属店铺不存在或已停用");
        }
        return shop;
    }

    private String normalizedName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new BusinessException("商品分类名称不能为空");
        }
        return value;
    }
}
