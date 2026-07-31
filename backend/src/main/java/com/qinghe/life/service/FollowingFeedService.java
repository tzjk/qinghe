package com.qinghe.life.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.entity.ExploreLike;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Follow;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.mapper.ExploreLikeMapper;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.mapper.FollowMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.redis.RedisBusinessMetrics;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.PublicUserSummaryVO;
import com.qinghe.life.vo.ExplorePostVO;
import com.qinghe.life.vo.FollowingFeedVO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Redis-only derived social indexes. Database writes are always committed before this service mutates Redis. */
@Service
public class FollowingFeedService {
    private static final Logger log = LoggerFactory.getLogger(FollowingFeedService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int PUSH_PIPELINE_BATCH_SIZE = 200;
    private final ExplorePostMapper postMapper; private final ExploreLikeMapper likeMapper; private final FollowMapper followMapper; private final UserMapper userMapper; private final ShopMapper shopMapper; private final StringRedisTemplate redisTemplate; private final RedissonClient redisson; private final RedisBusinessMetrics metrics;
    private final int maxSize; private final int backfillSize;
    public FollowingFeedService(ExplorePostMapper postMapper, ExploreLikeMapper likeMapper, FollowMapper followMapper, UserMapper userMapper, ShopMapper shopMapper, StringRedisTemplate redisTemplate, RedissonClient redisson, RedisBusinessMetrics metrics, @Value("${qinghe.explore.feed.max-size:1000}") int maxSize, @Value("${qinghe.explore.feed.backfill-size:20}") int backfillSize) { this.postMapper = postMapper; this.likeMapper = likeMapper; this.followMapper = followMapper; this.userMapper = userMapper; this.shopMapper = shopMapper; this.redisTemplate = redisTemplate; this.redisson = redisson; this.metrics = metrics; this.maxSize = Math.max(100, maxSize); this.backfillSize = Math.max(10, Math.min(20, backfillSize)); }

    public void publishedAfterCommit(final ExplorePost post) { afterCommit(new Runnable() { @Override public void run() { pushToFollowers(post); } }); }
    public void likedAfterCommit(final Long postId, final ExploreLike like) { afterCommit(new Runnable() { @Override public void run() { try { redisTemplate.opsForZSet().addIfAbsent(RedisKeys.exploreLikers(postId), likerMember(like), timestamp(like.getCreatedAt())); } catch (Exception exception) { metric("liker_zset_rebuild_total", "like_write_failure", exception); } } }); }
    public void unlikedAfterCommit(final Long postId, final ExploreLike like) { afterCommit(new Runnable() { @Override public void run() { try { redisTemplate.opsForZSet().remove(RedisKeys.exploreLikers(postId), likerMember(like)); } catch (Exception exception) { metric("liker_zset_rebuild_total", "unlike_remove_failure", exception); } } }); }

    public List<PublicUserSummaryVO> topLikers(Long postId) {
        List<Long> ids;
        try { ids = cachedLikerIds(postId); } catch (Exception exception) { ids = databaseLikerIds(postId); metric("liker_zset_rebuild_total", "fallback", exception); }
        return publicUsers(ids);
    }
    public void backfill(Long recipientUserId, Long authorUserId) { try { List<ExplorePost> posts = postMapper.selectList(Wrappers.<ExplorePost>lambdaQuery().eq(ExplorePost::getUserId, authorUserId).eq(ExplorePost::getPostStatus, "PUBLISHED").orderByDesc(ExplorePost::getCreatedAt).last("LIMIT " + backfillSize)); push(recipientUserId, posts); metrics.count("feed_backfill_total", "explore_social", "backfill", "success", "none"); } catch (Exception exception) { metric("feed_push_failure_total", "backfill_failure", exception); } }
    public void cleanupAuthor(Long recipientUserId, Long authorUserId) { try { List<ExplorePost> posts = postMapper.selectList(Wrappers.<ExplorePost>lambdaQuery().eq(ExplorePost::getUserId, authorUserId).orderByDesc(ExplorePost::getCreatedAt).last("LIMIT " + backfillSize)); if (!posts.isEmpty()) { String[] ids = new String[posts.size()]; for (int i = 0; i < posts.size(); i++) ids[i] = String.valueOf(posts.get(i).getId()); redisTemplate.opsForZSet().remove(RedisKeys.followingFeed(recipientUserId), (Object[]) ids); } } catch (Exception exception) { metric("feed_push_failure_total", "unfollow_cleanup_failure", exception); } }
    public Set<Long> followingFromDatabase(Long userId) { Set<Long> ids = new HashSet<Long>(); for (Follow follow : followMapper.selectList(Wrappers.<Follow>lambdaQuery().eq(Follow::getUserId, userId))) ids.add(follow.getFollowUserId()); return ids; }
    public FollowingFeedVO followingFeed(Long userId, Long maxTime, Long offset, int requestedSize) {
        int size = Math.max(1, Math.min(50, requestedSize)); long ceiling = maxTime == null ? Long.MAX_VALUE : maxTime.longValue(); long skip = offset == null ? 0L : Math.min((long) maxSize, Math.max(0L, offset.longValue()));
        try { Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(RedisKeys.followingFeed(userId), 0D, (double) ceiling, skip, size); return fromRedisTuples(userId, tuples, ceiling, skip, size); }
        catch (Exception exception) { metrics.count("feed_fallback_total", "explore_social", "following_feed", "fallback", exception.getClass().getSimpleName()); return fallbackFeed(userId, ceiling, skip, size); }
    }

    private FollowingFeedVO fromRedisTuples(Long userId, Set<ZSetOperations.TypedTuple<String>> tuples, long ceiling, long skip, int size) {
        if (tuples == null || tuples.isEmpty()) return feed(Collections.<ExplorePostVO>emptyList(), ceiling, skip, false);
        List<Long> ids = new ArrayList<Long>(); long lastScore = ceiling; int sameScore = 0;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) { try { ids.add(Long.valueOf(tuple.getValue())); } catch (Exception ignored) { } if (tuple.getScore() != null) { long score = tuple.getScore().longValue(); if (score == lastScore) sameScore++; else { lastScore = score; sameScore = 1; } } }
        List<ExplorePostVO> rows = visiblePosts(ids, userId); Set<Long> visible = new HashSet<Long>(); for (ExplorePostVO row : rows) visible.add(row.getId()); for (Long id : ids) if (!visible.contains(id)) try { redisTemplate.opsForZSet().remove(RedisKeys.followingFeed(userId), String.valueOf(id)); } catch (Exception ignored) { }
        long nextOffset = lastScore == ceiling ? skip + sameScore : sameScore;
        return feed(rows, lastScore, nextOffset, tuples.size() >= size);
    }
    private FollowingFeedVO fallbackFeed(Long userId, long ceiling, long skip, int size) {
        Set<Long> following = followingFromDatabase(userId); if (following.isEmpty()) return feed(Collections.<ExplorePostVO>emptyList(), ceiling, skip, false); if (following.size() > 500) { List<Long> limited = new ArrayList<Long>(following); Collections.sort(limited); following = new HashSet<Long>(limited.subList(0, 500)); }
        LocalDateTime latest = ceiling == Long.MAX_VALUE ? LocalDateTime.now(ZONE) : LocalDateTime.ofInstant(Instant.ofEpochMilli(ceiling), ZONE);
        int candidateLimit = (int) Math.min((long) maxSize, skip + size * 3L);
        List<ExplorePost> posts = postMapper.selectList(Wrappers.<ExplorePost>lambdaQuery().in(ExplorePost::getUserId, following).eq(ExplorePost::getPostStatus, "PUBLISHED").le(ExplorePost::getCreatedAt, latest).orderByDesc(ExplorePost::getCreatedAt).orderByDesc(ExplorePost::getId).last("LIMIT " + candidateLimit));
        List<ExplorePost> page = new ArrayList<ExplorePost>(); long skippedAtCeiling = 0L;
        for (ExplorePost post : posts) { long score = timestamp(post.getCreatedAt()); if (score == ceiling && skippedAtCeiling++ < skip) continue; page.add(post); if (page.size() == size) break; }
        List<Long> ids = new ArrayList<Long>(); for (ExplorePost post : page) ids.add(post.getId()); long next = page.isEmpty() ? ceiling : timestamp(page.get(page.size() - 1).getCreatedAt()); long sameScore = 0L; for (int index = page.size() - 1; index >= 0 && timestamp(page.get(index).getCreatedAt()) == next; index--) sameScore++; long nextOffset = next == ceiling ? skip + sameScore : sameScore;
        return feed(visiblePosts(ids, userId), next, nextOffset, !page.isEmpty() && posts.size() >= candidateLimit);
    }
    private FollowingFeedVO feed(List<ExplorePostVO> rows, long minTime, long offset, boolean hasMore) { FollowingFeedVO view = new FollowingFeedVO(); view.setRecords(rows); view.setMinTime(minTime); view.setOffset(offset); view.setHasMore(hasMore); return view; }
    private List<ExplorePostVO> visiblePosts(List<Long> ids, Long viewerId) {
        if (ids.isEmpty()) return Collections.emptyList(); Set<Long> following = followingFromDatabase(viewerId); Map<Long, ExplorePost> posts = new HashMap<Long, ExplorePost>(); for (ExplorePost post : postMapper.selectBatchIds(ids)) if ("PUBLISHED".equals(post.getPostStatus()) && following.contains(post.getUserId())) posts.put(post.getId(), post);
        Set<Long> authorIds = new HashSet<Long>(); Set<Long> shopIds = new HashSet<Long>(); for (ExplorePost post : posts.values()) { authorIds.add(post.getUserId()); if (post.getShopId() != null) shopIds.add(post.getShopId()); }
        Map<Long, User> users = new HashMap<Long, User>(); for (User user : userMapper.selectBatchIds(authorIds)) if (Integer.valueOf(1).equals(user.getStatus())) users.put(user.getId(), user);
        Map<Long, Shop> shops = new HashMap<Long, Shop>(); for (Shop shop : shopMapper.selectBatchIds(shopIds)) shops.put(shop.getId(), shop);
        List<ExplorePostVO> result = new ArrayList<ExplorePostVO>(); for (Long id : ids) { ExplorePost post = posts.get(id); User author = post == null ? null : users.get(post.getUserId()); if (post == null || author == null) continue; ExplorePostVO view = new ExplorePostVO(); view.setId(post.getId()); view.setUserId(post.getUserId()); view.setAuthorName(author.getNickname()); view.setAuthorAvatar(author.getAvatarUrl()); view.setShopId(post.getShopId()); view.setShopName(shops.containsKey(post.getShopId()) ? shops.get(post.getShopId()).getName() : "店铺已归档"); view.setTitle(post.getTitle()); view.setContent(post.getContent()); view.setLikeCount(post.getLikeCount() == null ? 0 : post.getLikeCount()); view.setCommentCount(post.getCommentCount() == null ? 0 : post.getCommentCount()); view.setLiked(likeMapper.selectCount(Wrappers.<ExploreLike>lambdaQuery().eq(ExploreLike::getPostId, post.getId()).eq(ExploreLike::getUserId, viewerId)) > 0); view.setTopLikers(topLikers(post.getId())); view.setPostStatus(post.getPostStatus()); view.setCreatedAt(post.getCreatedAt()); view.setUpdatedAt(post.getUpdatedAt()); result.add(view); } return result;
    }

    private void pushToFollowers(ExplorePost post) {
        if (post == null || !"PUBLISHED".equals(post.getPostStatus())) return;
        try { long page = 1L; while (true) { IPage<Follow> follows = followMapper.selectPage(new Page<Follow>(page, PUSH_PIPELINE_BATCH_SIZE, false), Wrappers.<Follow>lambdaQuery().eq(Follow::getFollowUserId, post.getUserId()).orderByAsc(Follow::getId)); if (follows.getRecords() == null || follows.getRecords().isEmpty()) break; pushBatch(follows.getRecords(), post); if (follows.getRecords().size() < PUSH_PIPELINE_BATCH_SIZE) break; page++; } metrics.count("feed_push_total", "explore_social", "publish", "success", "none"); }
        catch (Exception exception) { metric("feed_push_failure_total", "publish_failure", exception); }
    }
    private void push(Long recipientUserId, List<ExplorePost> posts) { if (posts == null || posts.isEmpty()) return; redisTemplate.executePipelined(new RedisCallback<Object>() { @Override public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) { for (ExplorePost post : posts) writeFeed(connection, recipientUserId, post); return null; } }); }
    private void pushBatch(final List<Follow> follows, final ExplorePost post) { redisTemplate.executePipelined(new RedisCallback<Object>() { @Override public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) { for (Follow follow : follows) writeFeed(connection, follow.getUserId(), post); return null; } }); }
    private void writeFeed(org.springframework.data.redis.connection.RedisConnection connection, Long recipientUserId, ExplorePost post) { byte[] key = RedisKeys.followingFeed(recipientUserId).getBytes(StandardCharsets.UTF_8); connection.zSetCommands().zAdd(key, timestamp(post.getCreatedAt()), String.valueOf(post.getId()).getBytes(StandardCharsets.UTF_8)); connection.execute("ZREMRANGEBYRANK", key, "0".getBytes(StandardCharsets.UTF_8), String.valueOf(-maxSize - 1L).getBytes(StandardCharsets.UTF_8)); }

    private List<Long> cachedLikerIds(Long postId) {
        String key = RedisKeys.exploreLikers(postId); String loaded = RedisKeys.exploreLikersLoaded(postId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key)) && Boolean.TRUE.equals(redisTemplate.hasKey(loaded))) return toIds(redisTemplate.opsForZSet().range(key, 0, 4));
        RLock lock = redisson.getLock(RedisKeys.exploreLikersLock(postId)); boolean acquired = false;
        try { acquired = lock.tryLock(100, TimeUnit.MILLISECONDS); if (!acquired) return databaseLikerIds(postId); if (!Boolean.TRUE.equals(redisTemplate.hasKey(key)) || !Boolean.TRUE.equals(redisTemplate.hasKey(loaded))) rebuildLikers(postId); return toIds(redisTemplate.opsForZSet().range(key, 0, 4)); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); return databaseLikerIds(postId); }
        finally { if (acquired && lock.isHeldByCurrentThread()) lock.unlock(); }
    }
    private void rebuildLikers(Long postId) { List<ExploreLike> likes = likeMapper.selectList(Wrappers.<ExploreLike>lambdaQuery().eq(ExploreLike::getPostId, postId).orderByAsc(ExploreLike::getCreatedAt).orderByAsc(ExploreLike::getId)); String key = RedisKeys.exploreLikers(postId); for (ExploreLike like : likes) redisTemplate.opsForZSet().addIfAbsent(key, likerMember(like), timestamp(like.getCreatedAt())); long ttl = 900L + ThreadLocalRandom.current().nextLong(0, 121); redisTemplate.expire(key, ttl, TimeUnit.SECONDS); redisTemplate.opsForValue().set(RedisKeys.exploreLikersLoaded(postId), "1", ttl, TimeUnit.SECONDS); metrics.count("liker_zset_rebuild_total", "explore_social", "likers", "success", "none"); }
    private List<Long> databaseLikerIds(Long postId) { List<ExploreLike> rows = likeMapper.selectList(Wrappers.<ExploreLike>lambdaQuery().eq(ExploreLike::getPostId, postId).orderByAsc(ExploreLike::getCreatedAt).orderByAsc(ExploreLike::getId).last("LIMIT 5")); List<Long> ids = new ArrayList<Long>(); for (ExploreLike item : rows) ids.add(item.getUserId()); return ids; }
    private List<Long> toIds(Set<String> members) { if (members == null) return Collections.emptyList(); List<Long> ids = new ArrayList<Long>(); for (String member : members) try { int separator = member == null ? -1 : member.indexOf(':'); ids.add(Long.valueOf(separator < 0 ? member : member.substring(separator + 1))); } catch (NumberFormatException ignored) { } return ids; }
    private String likerMember(ExploreLike like) { return String.format("%020d:%d", like.getId(), like.getUserId()); }
    private List<PublicUserSummaryVO> publicUsers(List<Long> ids) { if (ids.isEmpty()) return Collections.emptyList(); Map<Long, User> users = new HashMap<Long, User>(); for (User user : userMapper.selectBatchIds(ids)) if (Integer.valueOf(1).equals(user.getStatus())) users.put(user.getId(), user); List<PublicUserSummaryVO> result = new ArrayList<PublicUserSummaryVO>(); for (Long id : ids) { User user = users.get(id); if (user == null) continue; PublicUserSummaryVO item = new PublicUserSummaryVO(); item.setUserId(user.getId()); item.setNickname(user.getNickname()); item.setAvatar(user.getAvatarUrl()); result.add(item); } return result; }
    private long timestamp(LocalDateTime value) { return (value == null ? LocalDateTime.now() : value).atZone(ZONE).toInstant().toEpochMilli(); }
    private void afterCommit(Runnable work) { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { work.run(); } }); else work.run(); }
    private void metric(String name, String operation, Exception exception) { metrics.count(name, "explore_social", operation, "failure", exception.getClass().getSimpleName()); log.warn("社交派生 Redis 操作失败，operation={}, type={}", operation, exception.getClass().getSimpleName()); }
}
