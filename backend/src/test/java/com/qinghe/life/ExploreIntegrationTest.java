package com.qinghe.life;

import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Shop;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExploreIntegrationTest extends ExploreTestSupport {
    @Test void rejectsUnauthenticatedPublishAndInvalidFields() throws Exception {
        Shop shop = shop(MARKER + "SHOP_AUTH", category, "116.300001", "39.900001", 1);
        mvc.perform(post("/api/explore/posts").contentType("application/json").content("{\"shopId\":" + shop.getId() + ",\"title\":\"x\",\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/explore/posts").header("Authorization", userAuthorization(USER_ONE_TOKEN)).contentType("application/json").content("{\"shopId\":" + shop.getId() + ",\"title\":\"\",\"content\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
    @Test void publishesAndListsLatestFirst() throws Exception {
        Shop shop = shop(MARKER + "SHOP_LATEST", category, "116.300002", "39.900002", 1);
        ExplorePost older = seedPost(userOne, shop, MARKER + "OLDER", 0, LocalDateTime.now().minusHours(1));
        mvc.perform(post("/api/explore/posts").header("Authorization", userAuthorization(USER_ONE_TOKEN)).contentType("application/json").content("{\"shopId\":" + shop.getId() + ",\"title\":\"" + MARKER + "NEW\",\"content\":\"正文\",\"images\":[\"https://oss.example/a.webp\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.title").value(MARKER + "NEW"));
        mvc.perform(get("/api/explore/posts").param("sort", "latest").param("shopId", String.valueOf(shop.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].title").value(MARKER + "NEW"));
        assertEquals(older.getId(), postMapper.selectById(older.getId()).getId());
    }
    @Test void hotSortFallsBackToMysqlWhenRealRedisKeyHasWrongType() throws Exception {
        Shop shop = shop(MARKER + "SHOP_HOT", category, "116.300003", "39.900003", 1);
        seedPost(userOne, shop, MARKER + "HOT_LOW", 1, LocalDateTime.now().minusMinutes(1));
        seedPost(userOne, shop, MARKER + "HOT_HIGH", 9, LocalDateTime.now().minusMinutes(2));
        redis.opsForValue().set(com.qinghe.life.utils.RedisKeys.exploreHot(), "wrong-type-for-this-test");
        try {
            mvc.perform(get("/api/explore/posts").param("sort", "hot").param("shopId", String.valueOf(shop.getId())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].title").value(MARKER + "HOT_HIGH"));
        } finally {
            redis.delete(com.qinghe.life.utils.RedisKeys.exploreHot());
        }
    }
    @Test void nonOwnerCannotEditAndDisabledPostIsHiddenFromUser() throws Exception {
        Shop shop = shop(MARKER + "SHOP_MOD", category, "116.300004", "39.900004", 1);
        ExplorePost item = seedPost(userOne, shop, MARKER + "MOD", 0, LocalDateTime.now());
        mvc.perform(put("/api/explore/posts/{id}", item.getId()).contentType("application/json").content("{\"shopId\":" + shop.getId() + ",\"title\":\"" + MARKER + "EDIT\",\"content\":\"正文\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/explore/posts/{id}", item.getId())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/explore/posts/{id}", item.getId()).header("Authorization", userAuthorization(USER_TWO_TOKEN)).contentType("application/json").content("{\"shopId\":" + shop.getId() + ",\"title\":\"" + MARKER + "EDIT\",\"content\":\"正文\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/explore/posts/{id}", item.getId()).header("Authorization", userAuthorization(USER_TWO_TOKEN)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/explore/posts/{id}/status", item.getId()).header("Authorization", adminAuthorization()).contentType("application/json").content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/api/explore/posts/{id}", item.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
    }
}
