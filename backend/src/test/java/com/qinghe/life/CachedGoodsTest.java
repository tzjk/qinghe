package com.qinghe.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.cache.CachedGoods;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.vo.GoodsVO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedGoodsTest {
    @Test
    void legacyCachedEntryUsesCurrentPresentationFields() {
        Goods current = goods();

        GoodsVO result = new CachedGoods().toGoodsVO(current);

        assertEquals("真实商品描述", result.getDescription());
        assertEquals(new BigDecimal("5.00"), result.getPrice());
        assertEquals("https://cdn.example/goods.webp", result.getCoverImage());
    }

    @Test
    void cacheSnapshotExposesPresentationFields() {
        CachedGoods cached = CachedGoods.from(goods(), "零食");

        assertEquals("真实商品描述", cached.getDescription());
        assertEquals(new BigDecimal("5.00"), cached.getPrice());
        assertEquals("https://cdn.example/goods.webp", cached.getCoverImage());
    }

    @Test
    void cacheSerializationKeepsPresentationFields() throws Exception {
        String json = new ObjectMapper().writeValueAsString(CachedGoods.from(goods(), "零食"));

        assertTrue(json.contains("\"description\":\"真实商品描述\""));
        assertTrue(json.contains("\"price\":5.00"));
        assertTrue(json.contains("\"coverImage\":\"https://cdn.example/goods.webp\""));
    }

    private Goods goods() {
        Goods goods = new Goods();
        goods.setId(124L);
        goods.setShopId(88L);
        goods.setName("好丽友派");
        goods.setDescription("真实商品描述");
        goods.setPrice(new BigDecimal("5.00"));
        goods.setStock(50);
        goods.setSalesCount(0);
        goods.setSaleStatus("ON_SALE");
        goods.setCoverImage("https://cdn.example/goods.webp");
        return goods;
    }
}
