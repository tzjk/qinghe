package com.qinghe.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.impl.ShopServiceImpl;
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

class ShopCoverServiceTest {
    @Test
    void databaseFailureCompensatesNewObjectAndExternalOldCoverIsNotDeleted() {
        ShopMapper shops = mock(ShopMapper.class);
        CategoryMapper categories = mock(CategoryMapper.class);
        AliyunOSSOperator oss = mock(AliyunOSSOperator.class);
        Shop shop = shop("https://external.example/default.png");
        when(shops.selectById(1L)).thenReturn(shop);
        when(shops.updateById(any(Shop.class))).thenReturn(0);
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + invocation.getArgument(0));
        ShopServiceImpl service = new ShopServiceImpl(shops, null, null, null, categories, null, new ObjectMapper(), oss);

        assertThrows(BusinessException.class, () -> service.uploadAdminShopCover(1L, pngFile()));
        verify(oss).deleteObject(anyString());
        verify(oss, never()).deleteObject("https://external.example/default.png");
    }

    @Test
    void onlyAcceptedCurrentShopPrefixCanBeDeletedAfterSuccessfulReplacement() {
        ShopMapper shops = mock(ShopMapper.class);
        CategoryMapper categories = mock(CategoryMapper.class);
        AliyunOSSOperator oss = mock(AliyunOSSOperator.class);
        String oldKey = "qinghe-life-service/shops/2026/07/123e4567-e89b-12d3-a456-426614174000.webp";
        String oldUrl = "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + oldKey;
        Shop shop = shop(oldUrl);
        when(shops.selectById(1L)).thenReturn(shop);
        when(shops.updateById(any(Shop.class))).thenReturn(1);
        when(categories.selectById(1L)).thenReturn(category());
        when(oss.ownShopCoverKey(oldUrl)).thenReturn(oldKey);
        when(oss.upload(anyString(), any(java.io.InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://java-ai1-kevin.oss-cn-beijing.aliyuncs.com/" + invocation.getArgument(0));
        ShopServiceImpl service = new ShopServiceImpl(shops, null, null, null, categories, null, new ObjectMapper(), oss);

        service.uploadAdminShopCover(1L, pngFile());
        verify(oss).deleteObject(oldKey);
    }

    private Shop shop(String coverUrl) {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setCategoryId(1L);
        shop.setName("测试店铺");
        shop.setCoverImage(coverUrl);
        return shop;
    }

    private Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setName("测试分类");
        category.setStatus(1);
        return category;
    }

    private MockMultipartFile pngFile() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL3vgAAAABJRU5ErkJggg==");
        return new MockMultipartFile("file", "cover.png", "image/png", png);
    }
}
