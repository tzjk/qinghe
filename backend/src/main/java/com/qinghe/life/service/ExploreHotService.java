package com.qinghe.life.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.utils.RedisKeys;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ExploreHotService {
    private static final Logger log = LoggerFactory.getLogger(ExploreHotService.class);
    private final StringRedisTemplate redisTemplate;
    private final ExplorePostMapper postMapper;

    public ExploreHotService(StringRedisTemplate redisTemplate, ExplorePostMapper postMapper) { this.redisTemplate = redisTemplate; this.postMapper = postMapper; }

    public void updateAfterCommit(final Long postId, final int delta, final int databaseLikeCount) {
        Runnable work = new Runnable() { @Override public void run() { updateScore(postId, delta, databaseLikeCount); } };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { work.run(); } });
        } else work.run();
    }

    public List<Long> rankedPostIds(long limit) {
        try {
            Set<String> values = redisTemplate.opsForZSet().reverseRange(RedisKeys.EXPLORE_HOT, 0, limit - 1);
            if (values == null || values.isEmpty()) { rebuild(); return Collections.emptyList(); }
            List<Long> ids = new ArrayList<Long>();
            for (String value : values) try { ids.add(Long.valueOf(value)); } catch (NumberFormatException ignored) { }
            return ids;
        } catch (Exception exception) { log.warn("读取探店热门排行失败，将回退数据库，type={}", exception.getClass().getSimpleName()); return null; }
    }

    /** Clears and rebuilds only qh:zset:explore:hot from MySQL like_count; safe to invoke after cache loss. */
    public void rebuild() {
        try {
            List<ExplorePost> posts = postMapper.selectList(Wrappers.<ExplorePost>lambdaQuery().eq(ExplorePost::getPostStatus, "PUBLISHED").orderByDesc(ExplorePost::getLikeCount).last("LIMIT 500"));
            redisTemplate.delete(RedisKeys.EXPLORE_HOT);
            for (ExplorePost post : posts) redisTemplate.opsForZSet().add(RedisKeys.EXPLORE_HOT, String.valueOf(post.getId()), Math.max(0, post.getLikeCount() == null ? 0 : post.getLikeCount()));
        } catch (Exception exception) { log.warn("重建探店热门排行失败，type={}", exception.getClass().getSimpleName()); }
    }

    private void updateScore(Long postId, int delta, int databaseLikeCount) {
        try {
            Double score = redisTemplate.opsForZSet().score(RedisKeys.EXPLORE_HOT, String.valueOf(postId));
            if (score == null) redisTemplate.opsForZSet().add(RedisKeys.EXPLORE_HOT, String.valueOf(postId), Math.max(0, databaseLikeCount));
            else redisTemplate.opsForZSet().incrementScore(RedisKeys.EXPLORE_HOT, String.valueOf(postId), delta);
            Double current = redisTemplate.opsForZSet().score(RedisKeys.EXPLORE_HOT, String.valueOf(postId));
            if (current != null && current.doubleValue() < 0D) redisTemplate.opsForZSet().add(RedisKeys.EXPLORE_HOT, String.valueOf(postId), 0D);
        } catch (Exception exception) { log.warn("更新探店热门排行失败，postId={}, type={}", postId, exception.getClass().getSimpleName()); }
    }
}
