package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.entity.Follow;
import com.qinghe.life.entity.User;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.FollowMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.service.FollowService;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.FollowCountsVO;
import com.qinghe.life.vo.PublicUserSummaryVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class FollowServiceImpl implements FollowService {
    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);
    private static final long CACHE_TTL_SECONDS = 900L;
    private static final int COMMON_LIMIT = 100;
    private final FollowMapper followMapper; private final UserMapper userMapper; private final StringRedisTemplate redisTemplate; private final RedissonClient redisson; private final RedisBusinessMetrics metrics; private final FollowingFeedService feedService;
    public FollowServiceImpl(FollowMapper followMapper, UserMapper userMapper, StringRedisTemplate redisTemplate, RedissonClient redisson, RedisBusinessMetrics metrics, FollowingFeedService feedService) { this.followMapper = followMapper; this.userMapper = userMapper; this.redisTemplate = redisTemplate; this.redisson = redisson; this.metrics = metrics; this.feedService = feedService; }

    @Override @Transactional(rollbackFor = Exception.class) public FollowCountsVO follow(final Long targetUserId) {
        final Long userId = requireUser(); validateTarget(userId, targetUserId);
        boolean created = false;
        try { Follow item = new Follow(); item.setUserId(userId); item.setFollowUserId(targetUserId); created = followMapper.insert(item) == 1; }
        catch (DuplicateKeyException ignored) { }
        final boolean changed = created;
        afterCommit(new Runnable() { @Override public void run() { if (changed) { metrics.count("follow_create_total", "explore_social", "follow", "success", "none"); updateMembership(userId, targetUserId, true); feedService.backfill(userId, targetUserId); } } });
        return counts(userId, targetUserId);
    }

    @Override @Transactional(rollbackFor = Exception.class) public FollowCountsVO unfollow(final Long targetUserId) {
        final Long userId = requireUser(); validateExistingTarget(targetUserId);
        boolean removed = followMapper.delete(Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, targetUserId)) > 0;
        final boolean changed = removed;
        afterCommit(new Runnable() { @Override public void run() { if (changed) { metrics.count("follow_remove_total", "explore_social", "unfollow", "success", "none"); updateMembership(userId, targetUserId, false); feedService.cleanupAuthor(userId, targetUserId); } } });
        return counts(userId, targetUserId);
    }

    @Override public FollowCountsVO relation(Long targetUserId) { Long userId = requireUser(); validateExistingTarget(targetUserId); return counts(userId, targetUserId); }
    @Override public PageResult<PublicUserSummaryVO> following(Long userId, long page, long size) { validateExistingTarget(userId); IPage<Follow> rows = followMapper.selectPage(new Page<Follow>(page, size), Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId).orderByDesc(Follow::getCreatedAt)); return page(rows.getRecords(), rows.getTotal(), rows.getCurrent(), rows.getSize(), requireUser()); }
    @Override public PageResult<PublicUserSummaryVO> followers(Long userId, long page, long size) { validateExistingTarget(userId); IPage<Follow> rows = followMapper.selectPage(new Page<Follow>(page, size), Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowUserId, userId).orderByDesc(Follow::getCreatedAt)); return pageFollowers(rows.getRecords(), rows.getTotal(), rows.getCurrent(), rows.getSize(), requireUser()); }

    @Override public List<PublicUserSummaryVO> common(Long targetUserId) {
        Long userId = requireUser(); validateExistingTarget(targetUserId); metrics.count("common_follow_query_total", "explore_social", "common_follow", "attempt", "none");
        Set<Long> ids;
        try { ensureFollowingCache(userId); ensureFollowingCache(targetUserId); ids = toIds(redisTemplate.opsForSet().intersect(RedisKeys.followings(userId), RedisKeys.followings(targetUserId))); }
        catch (Exception exception) { ids = databaseIntersection(userId, targetUserId); metrics.count("common_follow_query_total", "explore_social", "common_follow", "fallback", errorType(exception)); }
        List<Long> limited = new ArrayList<Long>(ids); Collections.sort(limited); if (limited.size() > COMMON_LIMIT) limited = limited.subList(0, COMMON_LIMIT); return publicUsers(limited, userId);
    }

    @Override public Set<Long> followingIds(Long userId) { return cachedIds(userId, true); }
    @Override public Set<Long> followerIds(Long userId) { return cachedIds(userId, false); }

    private Set<Long> cachedIds(Long userId, boolean following) {
        try { return ensureCache(userId, following); }
        catch (Exception exception) { metrics.count("follow_cache_rebuild_total", "explore_social", following ? "followings" : "followers", "fallback", errorType(exception)); return databaseIds(userId, following); }
    }
    private void ensureFollowingCache(Long userId) { ensureCache(userId, true); }
    private Set<Long> ensureCache(Long userId, boolean following) {
        String key = following ? RedisKeys.followings(userId) : RedisKeys.followers(userId); String loaded = following ? RedisKeys.followingsLoaded(userId) : RedisKeys.followersLoaded(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(loaded))) return members(key);
        RLock lock = redisson.getLock(RedisKeys.followCacheLock(userId)); boolean acquired = false;
        try {
            acquired = lock.tryLock(100, TimeUnit.MILLISECONDS); if (!acquired) return databaseIds(userId, following);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(loaded))) return members(key);
            Set<Long> ids = databaseIds(userId, following); if (!ids.isEmpty()) redisTemplate.opsForSet().add(key, strings(ids));
            long ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(0, 121); redisTemplate.expire(key, ttl, TimeUnit.SECONDS); redisTemplate.opsForValue().set(loaded, "1", ttl, TimeUnit.SECONDS); metrics.count("follow_cache_rebuild_total", "explore_social", following ? "followings" : "followers", "success", "none"); return ids;
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); return databaseIds(userId, following); }
        finally { if (acquired && lock.isHeldByCurrentThread()) lock.unlock(); }
    }
    private Set<Long> members(String key) { Set<String> values = redisTemplate.opsForSet().members(key); Set<Long> ids = new HashSet<Long>(); if (values != null) for (String value : values) try { ids.add(Long.valueOf(value)); } catch (NumberFormatException ignored) { } return ids; }
    private Set<Long> toIds(Set<String> values) { Set<Long> ids = new HashSet<Long>(); if (values != null) for (String value : values) try { ids.add(Long.valueOf(value)); } catch (NumberFormatException ignored) { } return ids; }
    private Set<Long> databaseIds(Long userId, boolean following) { List<Follow> rows = followMapper.selectList(Wrappers.<Follow>lambdaQuery().eq(following ? Follow::getUserId : Follow::getFollowUserId, userId)); Set<Long> result = new HashSet<Long>(); for (Follow row : rows) result.add(following ? row.getFollowUserId() : row.getUserId()); return result; }
    private Set<Long> databaseIntersection(Long userId, Long targetUserId) { Set<Long> left = databaseIds(userId, true); left.retainAll(databaseIds(targetUserId, true)); return left; }
    private void updateMembership(Long userId, Long targetUserId, boolean added) { try { if (added) { redisTemplate.opsForSet().add(RedisKeys.followings(userId), String.valueOf(targetUserId)); redisTemplate.opsForSet().add(RedisKeys.followers(targetUserId), String.valueOf(userId)); } else { redisTemplate.opsForSet().remove(RedisKeys.followings(userId), String.valueOf(targetUserId)); redisTemplate.opsForSet().remove(RedisKeys.followers(targetUserId), String.valueOf(userId)); } } catch (Exception exception) { metrics.count("follow_cache_update_total", "explore_social", added ? "follow" : "unfollow", "failure", errorType(exception)); log.warn("关注缓存提交后更新失败，operation={}, type={}", added ? "add" : "remove", errorType(exception)); } }
    private PageResult<PublicUserSummaryVO> page(List<Follow> rows, long total, long page, long size, Long currentUser) { List<Long> ids = new ArrayList<Long>(); for (Follow row : rows) ids.add(row.getFollowUserId()); return new PageResult<PublicUserSummaryVO>(publicUsers(ids, currentUser), total, page, size); }
    private PageResult<PublicUserSummaryVO> pageFollowers(List<Follow> rows, long total, long page, long size, Long currentUser) { List<Long> ids = new ArrayList<Long>(); for (Follow row : rows) ids.add(row.getUserId()); return new PageResult<PublicUserSummaryVO>(publicUsers(ids, currentUser), total, page, size); }
    public List<PublicUserSummaryVO> publicUsers(List<Long> ids, Long currentUser) { if (ids == null || ids.isEmpty()) return Collections.emptyList(); Map<Long, User> users = new HashMap<Long, User>(); for (User item : userMapper.selectBatchIds(ids)) if (Integer.valueOf(1).equals(item.getStatus())) users.put(item.getId(), item); Set<Long> mine = currentUser == null ? Collections.<Long>emptySet() : followingIds(currentUser); List<PublicUserSummaryVO> result = new ArrayList<PublicUserSummaryVO>(); for (Long id : ids) { User user = users.get(id); if (user == null) continue; PublicUserSummaryVO view = new PublicUserSummaryVO(); view.setUserId(user.getId()); view.setNickname(user.getNickname()); view.setAvatar(user.getAvatarUrl()); view.setFollowedByMe(mine.contains(user.getId())); view.setFollowerCount(followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowUserId, user.getId()))); result.add(view); } return result; }
    private FollowCountsVO counts(Long userId, Long targetUserId) { FollowCountsVO view = new FollowCountsVO(); view.setUserId(targetUserId); view.setFollowingCount(followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, targetUserId))); view.setFollowerCount(followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowUserId, targetUserId))); view.setFollowedByMe(followMapper.selectCount(Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, targetUserId)) > 0); return view; }
    private void validateTarget(Long userId, Long targetUserId) { if (targetUserId == null || userId.equals(targetUserId)) throw new BusinessException(400, "不能关注自己"); validateExistingTarget(targetUserId); }
    private void validateExistingTarget(Long userId) { User user = userMapper.selectById(userId); if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(404, "目标用户不存在或不可公开"); }
    private Long requireUser() { Long id = UserContext.getUserId(); if (id == null) throw new BusinessException(401, "请先登录"); return id; }
    private void afterCommit(Runnable task) { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { task.run(); } }); else task.run(); }
    private String[] strings(Set<Long> ids) { String[] values = new String[ids.size()]; int index = 0; for (Long id : ids) values[index++] = String.valueOf(id); return values; }
    private String errorType(Exception exception) { return exception.getClass().getSimpleName(); }
}
