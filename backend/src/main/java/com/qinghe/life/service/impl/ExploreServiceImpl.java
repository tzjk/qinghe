package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminExplorePostQuery;
import com.qinghe.life.dto.ExploreCommentCreateRequest;
import com.qinghe.life.dto.ExplorePostQuery;
import com.qinghe.life.dto.ExplorePostSaveRequest;
import com.qinghe.life.dto.ExploreStatusRequest;
import com.qinghe.life.dto.NearbyShopQuery;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.entity.ExploreComment;
import com.qinghe.life.entity.ExplorePost;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.entity.User;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.exception.ForbiddenException;
import com.qinghe.life.mapper.ExploreCommentMapper;
import com.qinghe.life.mapper.ExploreLikeMapper;
import com.qinghe.life.mapper.ExplorePostMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.mapper.UserMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.ExploreHotService;
import com.qinghe.life.service.ExploreService;
import com.qinghe.life.service.FollowService;
import com.qinghe.life.service.FollowingFeedService;
import com.qinghe.life.utils.AdminContext;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.ExploreCommentVO;
import com.qinghe.life.vo.ExploreInteractionVO;
import com.qinghe.life.vo.ExplorePostVO;
import com.qinghe.life.vo.NearbyShopVO;
import com.qinghe.life.vo.FollowingFeedVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExploreServiceImpl implements ExploreService {
    private static final int GEO_CANDIDATE_LIMIT = 500;
    private final ExplorePostMapper postMapper;
    private final ExploreLikeMapper likeMapper;
    private final ExploreCommentMapper commentMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final ExploreHotService hotService;
    private final StringRedisTemplate redisTemplate;
    private final AliyunOSSOperator ossOperator;
    private final FollowingFeedService followingFeedService;
    private final FollowService followService;

    public ExploreServiceImpl(ExplorePostMapper postMapper, ExploreLikeMapper likeMapper, ExploreCommentMapper commentMapper, ShopMapper shopMapper, UserMapper userMapper, ExploreHotService hotService, StringRedisTemplate redisTemplate, AliyunOSSOperator ossOperator, FollowingFeedService followingFeedService, FollowService followService) {
        this.postMapper = postMapper; this.likeMapper = likeMapper; this.commentMapper = commentMapper; this.shopMapper = shopMapper; this.userMapper = userMapper; this.hotService = hotService; this.redisTemplate = redisTemplate; this.ossOperator = ossOperator; this.followingFeedService = followingFeedService; this.followService = followService;
    }

    @Override public PageResult<ExplorePostVO> page(ExplorePostQuery query) {
        if (!"latest".equalsIgnoreCase(query.getSort()) && !"hot".equalsIgnoreCase(query.getSort())) throw new BusinessException(400, "排序方式仅支持latest或hot");
        if ("hot".equalsIgnoreCase(query.getSort())) return hotPage(query);
        IPage<ExplorePost> page = postMapper.selectPage(new Page<ExplorePost>(query.getPage(), query.getSize()), Wrappers.<ExplorePost>lambdaQuery().eq(ExplorePost::getPostStatus, "PUBLISHED").eq(query.getShopId() != null, ExplorePost::getShopId, query.getShopId()).orderByDesc(ExplorePost::getCreatedAt).orderByDesc(ExplorePost::getId));
        return new PageResult<ExplorePostVO>(postViews(page.getRecords(), currentUserId()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override public ExplorePostVO detail(Long id) { ExplorePost post = publishedPost(id); return postView(post, currentUserId(), users(single(post.getUserId())), shops(single(post.getShopId()))); }

    @Override @Transactional(rollbackFor = Exception.class) public ExplorePostVO create(ExplorePostSaveRequest request) {
        Long userId = requireUser(); requireEnabledShop(request.getShopId()); ExplorePost post = new ExplorePost(); post.setUserId(userId); post.setShopId(request.getShopId()); post.setTitle(request.getTitle().trim()); post.setContent(request.getContent().trim()); post.setImages(joinImages(request.getImages())); post.setLikeCount(0); post.setCommentCount(0); post.setPostStatus("PUBLISHED");
        if (postMapper.insert(post) != 1) throw new BusinessException("探店内容发布失败，请稍后重试"); followingFeedService.publishedAfterCommit(post);
        return postView(post, userId, users(single(userId)), shops(single(request.getShopId())));
    }

    @Override public String uploadImage(MultipartFile file) {
        requireUser();
        UploadedImage image = validateUploadImage(file);
        LocalDate today = LocalDate.now();
        String objectKey = "qinghe-life-service/explore/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + imageExtension(image.contentType);
        try {
            return ossOperator.upload(objectKey, new ByteArrayInputStream(image.bytes), image.bytes.length, image.contentType);
        } catch (Exception exception) {
            throw new BusinessException(503, "探店图片上传服务暂不可用");
        }
    }

    @Override @Transactional(rollbackFor = Exception.class) public ExplorePostVO update(Long id, ExplorePostSaveRequest request) {
        Long userId = requireUser(); ExplorePost post = ownPost(id, userId); if (!"PUBLISHED".equals(post.getPostStatus())) throw new BusinessException("当前内容不可编辑"); requireEnabledShop(request.getShopId()); post.setShopId(request.getShopId()); post.setTitle(request.getTitle().trim()); post.setContent(request.getContent().trim()); post.setImages(joinImages(request.getImages())); postMapper.updateById(post); return postView(post, userId, users(single(userId)), shops(single(post.getShopId())));
    }

    @Override @Transactional(rollbackFor = Exception.class) public void delete(Long id) { ExplorePost post = ownPost(id, requireUser()); post.setPostStatus("DELETED"); postMapper.updateById(post); }

    @Override @Transactional(rollbackFor = Exception.class) public ExploreInteractionVO like(Long id) { Long userId = requireUser(); ExplorePost post = publishedPost(id); if (likeMapper.insertIgnore(id, userId) == 1) { postMapper.update(null, Wrappers.<ExplorePost>lambdaUpdate().eq(ExplorePost::getId, id).setSql("like_count = like_count + 1")); post = postMapper.selectById(id); hotService.updateAfterCommit(id, 1, positive(post.getLikeCount())); com.qinghe.life.entity.ExploreLike like = likeMapper.selectOne(Wrappers.<com.qinghe.life.entity.ExploreLike>lambdaQuery().eq(com.qinghe.life.entity.ExploreLike::getPostId, id).eq(com.qinghe.life.entity.ExploreLike::getUserId, userId)); if (like != null && like.getId() != null) followingFeedService.likedAfterCommit(id, like); } return interaction(postMapper.selectById(id), true); }

    @Override @Transactional(rollbackFor = Exception.class) public ExploreInteractionVO unlike(Long id) { Long userId = requireUser(); ExplorePost post = publishedPost(id); com.qinghe.life.entity.ExploreLike like = likeMapper.selectOne(Wrappers.<com.qinghe.life.entity.ExploreLike>lambdaQuery().eq(com.qinghe.life.entity.ExploreLike::getPostId, id).eq(com.qinghe.life.entity.ExploreLike::getUserId, userId)); if (likeMapper.deleteByPostAndUser(id, userId) == 1) { postMapper.update(null, Wrappers.<ExplorePost>lambdaUpdate().eq(ExplorePost::getId, id).setSql("like_count = GREATEST(like_count - 1, 0)")); post = postMapper.selectById(id); hotService.updateAfterCommit(id, -1, positive(post.getLikeCount())); if (like != null && like.getId() != null) followingFeedService.unlikedAfterCommit(id, like); } return interaction(postMapper.selectById(id), false); }

    @Override public PageResult<ExploreCommentVO> comments(Long id, PageQuery query) { publishedPost(id); IPage<ExploreComment> page = commentMapper.selectPage(new Page<ExploreComment>(query.getPage(), query.getSize()), Wrappers.<ExploreComment>lambdaQuery().eq(ExploreComment::getPostId, id).eq(ExploreComment::getCommentStatus, "PUBLISHED").orderByAsc(ExploreComment::getCreatedAt).orderByAsc(ExploreComment::getId)); return new PageResult<ExploreCommentVO>(commentViews(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize()); }

    @Override @Transactional(rollbackFor = Exception.class) public ExploreCommentVO comment(Long id, ExploreCommentCreateRequest request) { Long userId = requireUser(); publishedPost(id); ExploreComment comment = new ExploreComment(); comment.setPostId(id); comment.setUserId(userId); comment.setContent(request.getContent().trim()); comment.setCommentStatus("PUBLISHED"); if (commentMapper.insert(comment) != 1) throw new BusinessException("评论发布失败，请稍后重试"); postMapper.update(null, Wrappers.<ExplorePost>lambdaUpdate().eq(ExplorePost::getId, id).setSql("comment_count = comment_count + 1")); return commentView(comment, users(single(userId)).get(userId)); }

    @Override public PageResult<NearbyShopVO> nearby(NearbyShopQuery query) { try { return nearbyFromRedis(query); } catch (Exception ignored) { return nearbyFromMysql(query); } }
    @Override public FollowingFeedVO followingFeed(Long maxTime, Long offset, Integer size) { return followingFeedService.followingFeed(requireUser(), maxTime, offset, size == null ? 10 : size); }

    @Override public PageResult<ExplorePostVO> adminPage(AdminExplorePostQuery query) { requireAdmin(); IPage<ExplorePost> page = postMapper.selectPage(new Page<ExplorePost>(query.getPage(), query.getSize()), Wrappers.<ExplorePost>lambdaQuery().eq(query.getShopId() != null, ExplorePost::getShopId, query.getShopId()).eq(query.getUserId() != null, ExplorePost::getUserId, query.getUserId()).eq(query.getStatus() != null && !query.getStatus().trim().isEmpty(), ExplorePost::getPostStatus, query.getStatus()).like(query.getKeyword() != null && !query.getKeyword().trim().isEmpty(), ExplorePost::getTitle, query.getKeyword()).orderByDesc(ExplorePost::getCreatedAt)); return new PageResult<ExplorePostVO>(postViews(page.getRecords(), null), page.getTotal(), page.getCurrent(), page.getSize()); }
    @Override public ExplorePostVO adminDetail(Long id) { requireAdmin(); ExplorePost post = requirePost(id); return postView(post, null, users(single(post.getUserId())), shops(single(post.getShopId()))); }
    @Override public PageResult<ExploreCommentVO> adminComments(Long postId, PageQuery query) { requireAdmin(); requirePost(postId); IPage<ExploreComment> page = commentMapper.selectPage(new Page<ExploreComment>(query.getPage(), query.getSize()), Wrappers.<ExploreComment>lambdaQuery().eq(ExploreComment::getPostId, postId).orderByDesc(ExploreComment::getCreatedAt)); return new PageResult<ExploreCommentVO>(commentViews(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize()); }
    @Override @Transactional(rollbackFor = Exception.class) public void updatePostStatus(Long id, ExploreStatusRequest request) { requireAdmin(); ExplorePost post = requirePost(id); post.setPostStatus(request.getStatus()); postMapper.updateById(post); if ("PUBLISHED".equals(request.getStatus())) hotService.rebuild(); }
    @Override @Transactional(rollbackFor = Exception.class) public void updateCommentStatus(Long id, ExploreStatusRequest request) { requireAdmin(); ExploreComment comment = commentMapper.selectById(id); if (comment == null) throw new BusinessException(404, "评论不存在"); comment.setCommentStatus(request.getStatus()); commentMapper.updateById(comment); }

    private PageResult<ExplorePostVO> hotPage(ExplorePostQuery query) { long requested = Math.min(GEO_CANDIDATE_LIMIT, query.getPage() * query.getSize()); List<Long> ids = hotService.rankedPostIds(requested); if (ids == null || ids.isEmpty()) return hotFallback(query); List<ExplorePost> rows = postMapper.selectBatchIds(ids).stream().filter(item -> "PUBLISHED".equals(item.getPostStatus()) && (query.getShopId() == null || query.getShopId().equals(item.getShopId()))).collect(Collectors.toList()); final Map<Long, Integer> order = new HashMap<Long, Integer>(); for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i); Collections.sort(rows, new Comparator<ExplorePost>() { @Override public int compare(ExplorePost a, ExplorePost b) { return order.get(a.getId()).compareTo(order.get(b.getId())); } }); int from = (int) Math.min((query.getPage() - 1L) * query.getSize(), rows.size()); int to = (int) Math.min(from + query.getSize(), rows.size()); return new PageResult<ExplorePostVO>(postViews(rows.subList(from, to), currentUserId()), (long) rows.size(), query.getPage(), query.getSize()); }
    private PageResult<ExplorePostVO> hotFallback(ExplorePostQuery query) { IPage<ExplorePost> page = postMapper.selectPage(new Page<ExplorePost>(query.getPage(), query.getSize()), Wrappers.<ExplorePost>lambdaQuery().eq(ExplorePost::getPostStatus, "PUBLISHED").eq(query.getShopId() != null, ExplorePost::getShopId, query.getShopId()).orderByDesc(ExplorePost::getLikeCount).orderByDesc(ExplorePost::getCreatedAt).orderByDesc(ExplorePost::getId)); return new PageResult<ExplorePostVO>(postViews(page.getRecords(), currentUserId()), page.getTotal(), page.getCurrent(), page.getSize()); }
    private PageResult<NearbyShopVO> nearbyFromRedis(NearbyShopQuery query) { Circle circle = new Circle(new Point(query.getLongitude().doubleValue(), query.getLatitude().doubleValue()), new Distance(query.getRadius().doubleValue(), Metrics.KILOMETERS)); GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(RedisKeys.shopGeo(), circle, RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending().limit(GEO_CANDIDATE_LIMIT)); List<NearbyShopVO> rows = new ArrayList<NearbyShopVO>(); if (results != null) for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) { try { Shop shop = shopMapper.selectById(Long.valueOf(result.getContent().getName())); if (shop != null && Integer.valueOf(1).equals(shop.getStatus()) && (query.getCategoryId() == null || query.getCategoryId().equals(shop.getCategoryId()))) rows.add(nearbyView(shop, BigDecimal.valueOf(result.getDistance().getValue()))); } catch (NumberFormatException ignored) { } } return pageNearby(rows, query); }
    private PageResult<NearbyShopVO> nearbyFromMysql(NearbyShopQuery query) { List<NearbyShopVO> rows = new ArrayList<NearbyShopVO>(); for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().eq(Shop::getStatus, 1).eq(query.getCategoryId() != null, Shop::getCategoryId, query.getCategoryId()).isNotNull(Shop::getLongitude).isNotNull(Shop::getLatitude))) { BigDecimal distance = distance(query.getLatitude(), query.getLongitude(), shop.getLatitude(), shop.getLongitude()); if (distance.compareTo(query.getRadius()) <= 0) rows.add(nearbyView(shop, distance)); } Collections.sort(rows, new Comparator<NearbyShopVO>() { @Override public int compare(NearbyShopVO a, NearbyShopVO b) { return a.getDistance().compareTo(b.getDistance()); } }); return pageNearby(rows, query); }
    private PageResult<NearbyShopVO> pageNearby(List<NearbyShopVO> rows, NearbyShopQuery query) { int from = (int) Math.min((query.getPage() - 1L) * query.getSize(), rows.size()); int to = (int) Math.min(from + query.getSize(), rows.size()); return new PageResult<NearbyShopVO>(rows.subList(from, to), (long) rows.size(), query.getPage(), query.getSize()); }
    private NearbyShopVO nearbyView(Shop shop, BigDecimal distance) { NearbyShopVO view = new NearbyShopVO(); view.setId(shop.getId()); view.setCategoryId(shop.getCategoryId()); view.setName(shop.getName()); view.setAddress(shop.getAddress()); view.setCoverImage(shop.getCoverImage()); view.setScore(shop.getScore()); view.setDistance(distance.setScale(2, RoundingMode.HALF_UP)); return view; }
    private BigDecimal distance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) { double earth = 6371.0088D; double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue()); double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue()); double a = Math.sin(dLat / 2D) * Math.sin(dLat / 2D) + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) * Math.sin(dLon / 2D) * Math.sin(dLon / 2D); return BigDecimal.valueOf(earth * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a))); }
    private List<ExplorePostVO> postViews(List<ExplorePost> posts, Long currentUser) { if (posts.isEmpty()) return Collections.emptyList(); Set<Long> userIds = posts.stream().map(ExplorePost::getUserId).collect(Collectors.toSet()); Set<Long> shopIds = posts.stream().map(ExplorePost::getShopId).filter(item -> item != null).collect(Collectors.toSet()); Map<Long, User> users = users(userIds); Map<Long, Shop> shops = shops(shopIds); Set<Long> following = currentUser == null ? Collections.<Long>emptySet() : followService.followingIds(currentUser); return posts.stream().map(item -> postView(item, currentUser, users, shops, following)).collect(Collectors.toList()); }
    private ExplorePostVO postView(ExplorePost post, Long currentUser, Map<Long, User> users, Map<Long, Shop> shops) { return postView(post, currentUser, users, shops, currentUser == null ? Collections.<Long>emptySet() : followService.followingIds(currentUser)); }
    private ExplorePostVO postView(ExplorePost post, Long currentUser, Map<Long, User> users, Map<Long, Shop> shops, Set<Long> following) { ExplorePostVO view = new ExplorePostVO(); view.setId(post.getId()); view.setUserId(post.getUserId()); User user = users.get(post.getUserId()); view.setAuthorName(user == null ? "校园用户" : user.getNickname()); view.setAuthorAvatar(user == null ? null : user.getAvatarUrl()); view.setShopId(post.getShopId()); Shop shop = shops.get(post.getShopId()); view.setShopName(shop == null ? "店铺已归档" : shop.getName()); view.setTitle(post.getTitle()); view.setContent(post.getContent()); view.setImages(splitImages(post.getImages())); view.setLikeCount(positive(post.getLikeCount())); view.setCommentCount(positive(post.getCommentCount())); view.setTopLikers(followingFeedService.topLikers(post.getId())); view.setPostStatus(post.getPostStatus()); view.setCreatedAt(post.getCreatedAt()); view.setUpdatedAt(post.getUpdatedAt()); view.setLiked(currentUser != null && likeMapper.selectCount(Wrappers.<com.qinghe.life.entity.ExploreLike>lambdaQuery().eq(com.qinghe.life.entity.ExploreLike::getPostId, post.getId()).eq(com.qinghe.life.entity.ExploreLike::getUserId, currentUser)) > 0); view.setFollowedByMe(currentUser != null && following.contains(post.getUserId())); return view; }
    private List<ExploreCommentVO> commentViews(List<ExploreComment> comments) { Set<Long> ids = comments.stream().map(ExploreComment::getUserId).collect(Collectors.toSet()); Map<Long, User> users = users(ids); return comments.stream().map(item -> commentView(item, users.get(item.getUserId()))).collect(Collectors.toList()); }
    private ExploreCommentVO commentView(ExploreComment comment, User user) { ExploreCommentVO view = new ExploreCommentVO(); view.setId(comment.getId()); view.setPostId(comment.getPostId()); view.setUserId(comment.getUserId()); view.setAuthorName(user == null ? "校园用户" : user.getNickname()); view.setAuthorAvatar(user == null ? null : user.getAvatarUrl()); view.setContent(comment.getContent()); view.setCommentStatus(comment.getCommentStatus()); view.setCreatedAt(comment.getCreatedAt()); return view; }
    private ExploreInteractionVO interaction(ExplorePost post, boolean liked) { ExploreInteractionVO view = new ExploreInteractionVO(); view.setPostId(post.getId()); view.setLikeCount(positive(post.getLikeCount())); view.setLiked(liked); return view; }
    private ExplorePost publishedPost(Long id) { ExplorePost post = requirePost(id); if (!"PUBLISHED".equals(post.getPostStatus())) throw new BusinessException(404, "探店内容不存在或不可见"); return post; }
    private ExplorePost requirePost(Long id) { ExplorePost post = postMapper.selectById(id); if (post == null) throw new BusinessException(404, "探店内容不存在"); return post; }
    private ExplorePost ownPost(Long id, Long userId) { ExplorePost post = requirePost(id); if (!userId.equals(post.getUserId())) throw new ForbiddenException("只能操作自己发布的内容"); return post; }
    private void requireEnabledShop(Long id) { Shop shop = shopMapper.selectById(id); if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) throw new BusinessException("关联店铺不存在或未营业"); }
    private Long requireUser() { Long id = UserContext.getUserId(); if (id == null) throw new BusinessException(401, "请先登录"); return id; }
    private Long currentUserId() { return UserContext.getUserId(); }
    private UploadedImage validateUploadImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) throw new BusinessException("请选择有效图片");
        if (file.getSize() > 5L * 1024L * 1024L) throw new BusinessException("探店图片不能超过 5MB");
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp"))) throw new BusinessException("仅支持 jpg、jpeg、png 或 webp 图片");
        try {
            byte[] bytes = file.getBytes();
            String detectedType = detectedImageType(bytes);
            if (detectedType == null || !detectedType.equals(contentType)) throw new BusinessException("图片类型或文件内容不匹配");
            if ((fileName.endsWith(".png") && !"image/png".equals(detectedType))
                    || ((fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) && !"image/jpeg".equals(detectedType))
                    || (fileName.endsWith(".webp") && !"image/webp".equals(detectedType))) throw new BusinessException("图片扩展名与文件内容不匹配");
            return new UploadedImage(bytes, detectedType);
        } catch (IOException exception) {
            throw new BusinessException("图片读取失败，请重新选择");
        }
    }
    private String detectedImageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }
    private String imageExtension(String contentType) { return "image/jpeg".equals(contentType) ? ".jpg" : "image/png".equals(contentType) ? ".png" : ".webp"; }
    private static class UploadedImage { private final byte[] bytes; private final String contentType; private UploadedImage(byte[] bytes, String contentType) { this.bytes = bytes; this.contentType = contentType; } }
    private void requireAdmin() { if (AdminContext.getAdminId() == null) throw new BusinessException(401, "管理员登录已过期"); }
    private Map<Long, User> users(Set<Long> ids) { if (ids == null || ids.isEmpty()) return Collections.emptyMap(); Map<Long, User> result = new HashMap<Long, User>(); for (User user : userMapper.selectBatchIds(ids)) result.put(user.getId(), user); return result; }
    private Map<Long, Shop> shops(Set<Long> ids) { if (ids == null || ids.isEmpty()) return Collections.emptyMap(); Map<Long, Shop> result = new HashMap<Long, Shop>(); for (Shop shop : shopMapper.selectBatchIds(ids)) result.put(shop.getId(), shop); return result; }
    private Set<Long> single(Long id) { Set<Long> result = new HashSet<Long>(); if (id != null) result.add(id); return result; }
    private String joinImages(List<String> images) { if (images == null || images.isEmpty()) return null; List<String> cleaned = new ArrayList<String>(); for (String image : images) { String value = image == null ? null : image.trim(); if (value == null || value.isEmpty() || value.length() > 512) throw new BusinessException(400, "图片地址不正确"); cleaned.add(value); } return String.join(",", cleaned); }
    private List<String> splitImages(String images) { if (images == null || images.trim().isEmpty()) return Collections.emptyList(); List<String> result = new ArrayList<String>(); for (String image : images.split(",")) if (!image.trim().isEmpty()) result.add(image.trim()); return result; }
    private int positive(Integer value) { return value == null || value < 0 ? 0 : value; }
}
