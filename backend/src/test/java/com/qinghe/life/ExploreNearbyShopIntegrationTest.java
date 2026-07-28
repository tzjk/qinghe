package com.qinghe.life;

import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.utils.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExploreNearbyShopIntegrationTest extends ExploreTestSupport {
    @Test void geoReturnsEnabledShopsByDistanceAndExcludesDisabled() throws Exception {
        Shop near = shop(MARKER + "SHOP_NEAR", category, "116.300000", "39.900000", 1);
        Shop far = shop(MARKER + "SHOP_FAR", category, "116.320000", "39.900000", 1);
        Shop disabled = shop(MARKER + "SHOP_DISABLED", category, "116.300500", "39.900000", 0); geo(near); geo(far); geo(disabled);
        mvc.perform(get("/api/explore/shops/nearby").param("longitude", "116.300000").param("latitude", "39.900000").param("radius", "5").param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].id").value(near.getId())).andExpect(jsonPath("$.data.records[1].id").value(far.getId())).andExpect(jsonPath("$.data.total").value(2));
    }
    @Test void filtersCategoryAndValidatesRadiusAndPageSize() throws Exception {
        Category second = category(MARKER + "SECOND_CATEGORY"); Shop first = shop(MARKER + "SHOP_FIRST", category, "116.301000", "39.900000", 1); Shop secondShop = shop(MARKER + "SHOP_SECOND", second, "116.302000", "39.900000", 1); geo(first); geo(secondShop);
        mvc.perform(get("/api/explore/shops/nearby").param("longitude", "116.300000").param("latitude", "39.900000").param("radius", "5").param("categoryId", String.valueOf(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].id").value(secondShop.getId()));
        mvc.perform(get("/api/explore/shops/nearby").param("longitude", "116.3").param("latitude", "39.9").param("radius", "21").param("size", "51"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
    @Test void realRedisWrongTypeFallsBackToMysqlDistanceQuery() throws Exception {
        Shop near = shop(MARKER + "SHOP_FALLBACK_NEAR", category, "116.300000", "39.900000", 1); Shop far = shop(MARKER + "SHOP_FALLBACK_FAR", category, "116.315000", "39.900000", 1);
        redis.delete(RedisKeys.SHOP_GEO); redis.opsForValue().set(RedisKeys.SHOP_GEO, "wrong-type-for-this-test");
        try {
            mvc.perform(get("/api/explore/shops/nearby").param("longitude", "116.300000").param("latitude", "39.900000").param("radius", "5").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].id").value(near.getId())).andExpect(jsonPath("$.data.records[1].id").value(far.getId()));
        } finally {
            redis.delete(RedisKeys.SHOP_GEO);
        }
    }
}
