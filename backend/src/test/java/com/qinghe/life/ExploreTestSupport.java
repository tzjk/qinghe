package com.qinghe.life;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.Admin;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.ExploreComment;
import com.qinghe.life.entity.ExploreLike;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.AdminMapper;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.ExploreCommentMapper;
import com.qinghe.life.mapper.ExploreLikeMapper;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.utils.RedisKeys;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

abstract class ExploreTestSupport {
    protected static final String MARKER = "EXPLORE_TEST_";
    protected static final String USER_ONE_TOKEN = MARKER + "USER_ONE";
    protected static final String USER_TWO_TOKEN = MARKER + "USER_TWO";
    protected static final String ADMIN_TOKEN = MARKER + "ADMIN";
    @Autowired protected MockMvc mvc;
    @Autowired protected StringRedisTemplate redis;
    @Autowired protected UserMapper userMapper;
    @Autowired protected AdminMapper adminMapper;
    @Autowired protected CategoryMapper categoryMapper;
    @Autowired protected ShopMapper shopMapper;
    @Autowired protected ExplorePostMapper postMapper;
    @Autowired protected ExploreLikeMapper likeMapper;
    @Autowired protected ExploreCommentMapper commentMapper;
    protected User userOne;
    protected User userTwo;
    protected Admin admin;
    protected Category category;

    @BeforeEach void prepareExploreData() {
        cleanupExploreData();
        category = category(MARKER + "CATEGORY");
        userOne = user(MARKER + "USER_ONE"); userTwo = user(MARKER + "USER_TWO");
        admin = new Admin(); admin.setUsername(MARKER + "ADMIN"); admin.setDisplayName(MARKER + "ADMIN"); admin.setPasswordHash("unused"); admin.setStatus(1); adminMapper.insert(admin);
        userSession(USER_ONE_TOKEN, userOne); userSession(USER_TWO_TOKEN, userTwo); adminSession();
    }

    @AfterEach void clearExploreData() { cleanupExploreData(); }

    protected Category category(String name) { Category item = new Category(); item.setName(name); item.setStatus(1); item.setSortOrder(9999); categoryMapper.insert(item); return item; }
    protected User user(String name) { User item = new User(); item.setPhone("139" + String.format("%08d", Math.abs((name + System.nanoTime()).hashCode()) % 100000000)); item.setNickname(name); item.setProfileCompleted(1); item.setStatus(1); userMapper.insert(item); return item; }
    protected Shop shop(String name, Category type, String longitude, String latitude, int status) { Shop item = new Shop(); item.setCategoryId(type.getId()); item.setName(name); item.setAddress(MARKER + "ADDRESS"); item.setScore(new BigDecimal("4.80")); item.setStatus(status); item.setIsFeatured(0); item.setSortOrder(9999); item.setLongitude(new BigDecimal(longitude)); item.setLatitude(new BigDecimal(latitude)); shopMapper.insert(item); return item; }
    protected ExplorePost seedPost(User user, Shop shop, String title, int likes, LocalDateTime createdAt) { ExplorePost item = new ExplorePost(); item.setUserId(user.getId()); item.setShopId(shop.getId()); item.setTitle(title); item.setContent(MARKER + "CONTENT"); item.setLikeCount(likes); item.setCommentCount(0); item.setPostStatus("PUBLISHED"); item.setCreatedAt(createdAt); item.setUpdatedAt(createdAt); postMapper.insert(item); return item; }
    protected String userAuthorization(String token) { return "Bearer " + token; }
    protected String adminAuthorization() { return "Bearer " + ADMIN_TOKEN; }
    protected void geo(Shop shop) { redis.opsForGeo().add(RedisKeys.SHOP_GEO, new Point(shop.getLongitude().doubleValue(), shop.getLatitude().doubleValue()), String.valueOf(shop.getId())); }

    private void userSession(String token, User user) { Map<String, String> session = new HashMap<String, String>(); session.put("id", String.valueOf(user.getId())); session.put("nickname", user.getNickname()); redis.opsForHash().putAll(RedisKeys.token(token), session); }
    private void adminSession() { Map<String, String> session = new HashMap<String, String>(); session.put("adminId", String.valueOf(admin.getId())); session.put("username", admin.getUsername()); session.put("displayName", admin.getDisplayName()); redis.opsForHash().putAll(RedisKeys.adminToken(ADMIN_TOKEN), session); }
    private void cleanupExploreData() {
        clearExploreRedisKeys();
        if (postMapper == null) return;
        List<ExplorePost> posts = postMapper.selectList(Wrappers.<ExplorePost>lambdaQuery().likeRight(ExplorePost::getTitle, MARKER));
        List<Long> postIds = new ArrayList<Long>(); for (ExplorePost post : posts) postIds.add(post.getId());
        if (!postIds.isEmpty()) { commentMapper.delete(Wrappers.<ExploreComment>lambdaQuery().in(ExploreComment::getPostId, postIds)); likeMapper.delete(Wrappers.<ExploreLike>lambdaQuery().in(ExploreLike::getPostId, postIds)); postMapper.delete(Wrappers.<ExplorePost>lambdaQuery().in(ExplorePost::getId, postIds)); }
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().likeRight(Shop::getName, MARKER));
        for (Shop shop : shops) { shopMapper.deleteById(shop.getId()); }
        userMapper.delete(Wrappers.<User>lambdaQuery().likeRight(User::getNickname, MARKER));
        adminMapper.delete(Wrappers.<Admin>lambdaQuery().likeRight(Admin::getUsername, MARKER));
        categoryMapper.delete(Wrappers.<Category>lambdaQuery().likeRight(Category::getName, MARKER));
        redis.delete(RedisKeys.token(USER_ONE_TOKEN)); redis.delete(RedisKeys.token(USER_TWO_TOKEN)); redis.delete(RedisKeys.adminToken(ADMIN_TOKEN));
        clearExploreRedisKeys();
    }
    private void clearExploreRedisKeys() {
        if (redis != null) {
            redis.delete(RedisKeys.SHOP_GEO);
            redis.delete(RedisKeys.EXPLORE_HOT);
        }
    }
}
