package com.qinghe.life;

import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.impl.AdminGoodsServiceImpl;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsImageServiceTest {
    @Test
    void databaseFailureCompensatesNewObjectAndExternalImageIsNotDeleted() {
        GoodsMapper goods = mock(GoodsMapper.class);
        ShopMapper shops = mock(ShopMapper.class);
        AliyunOSSOperator oss = mock(AliyunOSSOperator.class);
        when(goods.selectById(1L)).thenReturn(goods("https://external.example/default.png"));
        when(goods.updateById(any(Goods.class))).thenReturn(0);
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://bucket.example/" + invocation.getArgument(0));
        AdminGoodsServiceImpl service = new AdminGoodsServiceImpl(goods, shops, oss);

        assertThrows(BusinessException.class, () -> service.image(1L, pngFile()));
        verify(oss).deleteObject(anyString());
        verify(oss, never()).deleteObject("https://external.example/default.png");
    }

    @Test
    void onlyCurrentGoodsPrefixIsDeletedAfterSuccessfulReplacement() {
        GoodsMapper goods = mock(GoodsMapper.class);
        ShopMapper shops = mock(ShopMapper.class);
        AliyunOSSOperator oss = mock(AliyunOSSOperator.class);
        String oldKey = "qinghe-life-service/goods/2026/07/123e4567-e89b-12d3-a456-426614174000.webp";
        String oldUrl = "https://bucket.example/" + oldKey;
        when(goods.selectById(1L)).thenReturn(goods(oldUrl));
        when(goods.updateById(any(Goods.class))).thenReturn(1);
        when(shops.selectById(1L)).thenReturn(enabledShop());
        when(oss.ownGoodsImageKey(oldUrl)).thenReturn(oldKey);
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://bucket.example/" + invocation.getArgument(0));
        AdminGoodsServiceImpl service = new AdminGoodsServiceImpl(goods, shops, oss);

        service.image(1L, pngFile());
        verify(oss).deleteObject(oldKey);
    }

    @Test
    void nonGoodsPrefixImageIsNotDeletedAfterSuccessfulReplacement() {
        GoodsMapper goods = mock(GoodsMapper.class);
        ShopMapper shops = mock(ShopMapper.class);
        AliyunOSSOperator oss = mock(AliyunOSSOperator.class);
        String nonGoodsUrl = "https://bucket.example/qinghe-life-service/shops/2026/07/123e4567-e89b-12d3-a456-426614174000.webp";
        when(goods.selectById(1L)).thenReturn(goods(nonGoodsUrl));
        when(goods.updateById(any(Goods.class))).thenReturn(1);
        when(shops.selectById(1L)).thenReturn(enabledShop());
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://bucket.example/" + invocation.getArgument(0));
        when(oss.ownGoodsImageKey(nonGoodsUrl)).thenReturn(null);
        AdminGoodsServiceImpl service = new AdminGoodsServiceImpl(goods, shops, oss);

        service.image(1L, pngFile());
        verify(oss, never()).deleteObject(nonGoodsUrl);
    }

    private Goods goods(String imageUrl) {
        Goods goods = new Goods();
        goods.setId(1L);
        goods.setShopId(1L);
        goods.setName("测试商品");
        goods.setCoverImage(imageUrl);
        return goods;
    }

    private Shop enabledShop() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("测试店铺");
        shop.setCategoryId(1L);
        shop.setStatus(1);
        return shop;
    }

    private MockMultipartFile pngFile() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL3vgAAAABJRU5ErkJggg==");
        return new MockMultipartFile("file", "goods.png", "image/png", png);
    }
}
