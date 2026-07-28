package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.ExploreLike;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Shop;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExploreInteractionIntegrationTest extends ExploreTestSupport {
    @Test void duplicateLikeIsIdempotentAndCountMatchesRecord() throws Exception {
        ExplorePost item = content();
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/explore/posts/{id}/like", item.getId()).header("Authorization", userAuthorization(USER_ONE_TOKEN))).andExpect(status().isOk()).andExpect(jsonPath("$.data.liked").value(true));
        assertEquals(1L, likeMapper.selectCount(Wrappers.<ExploreLike>lambdaQuery().eq(ExploreLike::getPostId, item.getId()).eq(ExploreLike::getUserId, userOne.getId())).longValue());
        assertEquals(1, postMapper.selectById(item.getId()).getLikeCount().intValue());
    }
    @Test void repeatedUnlikeIsIdempotentAndNeverMakesCountNegative() throws Exception {
        ExplorePost item = content();
        mvc.perform(post("/api/explore/posts/{id}/like", item.getId()).header("Authorization", userAuthorization(USER_ONE_TOKEN))).andExpect(status().isOk());
        for (int i = 0; i < 2; i++) mvc.perform(delete("/api/explore/posts/{id}/like", item.getId()).header("Authorization", userAuthorization(USER_ONE_TOKEN))).andExpect(status().isOk()).andExpect(jsonPath("$.data.likeCount").value(0));
        assertEquals(0L, likeMapper.selectCount(Wrappers.<ExploreLike>lambdaQuery().eq(ExploreLike::getPostId, item.getId())).longValue());
        assertEquals(0, postMapper.selectById(item.getId()).getLikeCount().intValue());
    }
    @Test void publishesAndQueriesVisibleComments() throws Exception {
        ExplorePost item = content();
        mvc.perform(post("/api/explore/posts/{id}/comments", item.getId()).header("Authorization", userAuthorization(USER_TWO_TOKEN)).contentType("application/json").content("{\"content\":\"" + MARKER + "COMMENT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value(MARKER + "COMMENT"));
        mvc.perform(get("/api/explore/posts/{id}/comments", item.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].content").value(MARKER + "COMMENT"));
        assertEquals(1, postMapper.selectById(item.getId()).getCommentCount().intValue());
    }
    private ExplorePost content() { Shop shop = shop(MARKER + "SHOP_INTERACTION_" + System.nanoTime(), category, "116.310000", "39.910000", 1); return seedPost(userOne, shop, MARKER + "POST_" + System.nanoTime(), 0, LocalDateTime.now()); }
}
