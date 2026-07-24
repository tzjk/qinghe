package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qinghe.life.cache.CachedGoods;
import com.qinghe.life.cache.CatalogCache;
import com.qinghe.life.config.CatalogCacheProperties;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminShopQuery;
import com.qinghe.life.dto.AdminShopSaveRequest;
import com.qinghe.life.dto.AdminShopStatusRequest;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.dto.ShopQuery;
import com.qinghe.life.dto.ShopGoodsQuery;
import com.qinghe.life.entity.Category;
import com.qinghe.life.entity.Comment;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.GoodsCategory;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.CategoryMapper;
import com.qinghe.life.mapper.CommentMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.GoodsCategoryMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.ShopService;
import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.AdminShopVO;
import com.qinghe.life.vo.CommentVO;
import com.qinghe.life.vo.GoodsVO;
import com.qinghe.life.vo.GoodsCategoryVO;
import com.qinghe.life.vo.ShopVO;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShopServiceImpl implements ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);
    private static final long MAX_COVER_BYTES = 3L * 1024L * 1024L;
    private static final int MAX_COVER_WIDTH = 1200;
    private static final int MAX_COVER_HEIGHT = 800;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final CatalogCache catalogCache;
    private final CatalogCacheProperties cacheProperties;
    private final AliyunOSSOperator ossOperator;

    public ShopServiceImpl(ShopMapper shopMapper, GoodsMapper goodsMapper, GoodsCategoryMapper goodsCategoryMapper, CommentMapper commentMapper,
                           CategoryMapper categoryMapper, CatalogCache catalogCache, CatalogCacheProperties cacheProperties,
                           AliyunOSSOperator ossOperator) {
        this.shopMapper = shopMapper;
        this.goodsMapper = goodsMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
        this.commentMapper = commentMapper;
        this.categoryMapper = categoryMapper;
        this.catalogCache = catalogCache;
        this.cacheProperties = cacheProperties;
        this.ossOperator = ossOperator;
    }

    @Override
    public PageResult<ShopVO> page(ShopQuery query) {
        Page<Shop> page = shopMapper.selectPage(new Page<Shop>(query.getPage(), query.getSize()),
                Wrappers.<Shop>lambdaQuery()
                        .eq(Shop::getStatus, 1)
                        .eq(query.getCategoryId() != null, Shop::getCategoryId, query.getCategoryId())
                        .like(query.getKeyword() != null && !query.getKeyword().trim().isEmpty(), Shop::getName, query.getKeyword())
                        .orderByDesc("score".equalsIgnoreCase(query.getSort()), Shop::getScore)
                        .orderByDesc("latest".equalsIgnoreCase(query.getSort()), Shop::getId)
                        .orderByAsc(!"score".equalsIgnoreCase(query.getSort()) && !"latest".equalsIgnoreCase(query.getSort()), Shop::getSortOrder));
        return pageResult(page, ShopVO::fromShop);
    }

    @Override
    public ShopVO detail(Long id) {
        try {
            return cachedDetail(id);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            return ShopVO.fromShop(requireEnabledShop(id));
        }
    }

    @Override
    public PageResult<GoodsVO> goods(Long id, ShopGoodsQuery query) {
        detail(id);
        if (query.getCategoryId() != null) {
            GoodsCategory category = goodsCategoryMapper.selectById(query.getCategoryId());
            if (category == null || !id.equals(category.getShopId()) || !Integer.valueOf(1).equals(category.getStatus())) {
                return new PageResult<GoodsVO>(java.util.Collections.<GoodsVO>emptyList(), 0L, query.getPage(), query.getSize());
            }
        }
        List<CachedGoods> cachedGoods = catalogCache.getList(RedisKeys.shopGoods(id), RedisKeys.shopGoodsLock(id), CachedGoods.class,
                cacheProperties.getListTtlMinutes(), () -> loadShopGoods(id));
        List<CachedGoods> filtered = cachedGoods.stream()
                .filter(goods -> query.getCategoryId() == null || query.getCategoryId().equals(goods.getCategoryId()))
                .filter(goods -> query.getKeyword() == null || query.getKeyword().trim().isEmpty()
                        || goods.getName().contains(query.getKeyword().trim()))
                .collect(Collectors.toList());
        long total = filtered.size();
        int fromIndex = (int) Math.min((query.getPage() - 1L) * query.getSize(), total);
        int toIndex = (int) Math.min(fromIndex + query.getSize(), total);
        List<CachedGoods> requested = filtered.subList(fromIndex, toIndex);
        if (requested.isEmpty()) {
            return new PageResult<GoodsVO>(Collections.<GoodsVO>emptyList(), total, query.getPage(), query.getSize());
        }
        Map<Long, Goods> currentGoods = new HashMap<Long, Goods>();
        for (Goods current : goodsMapper.selectBatchIds(requested.stream().map(CachedGoods::getId).collect(Collectors.toList()))) {
            if ("ON_SALE".equals(current.getSaleStatus()) && id.equals(current.getShopId())) {
                currentGoods.put(current.getId(), current);
            }
        }
        List<GoodsVO> records = new ArrayList<GoodsVO>();
        for (CachedGoods cached : requested) {
            Goods current = currentGoods.get(cached.getId());
            if (current != null) {
                records.add(cached.toGoodsVO(current));
            }
        }
        return new PageResult<GoodsVO>(records, total, query.getPage(), query.getSize());
    }

    @Override
    public List<GoodsCategoryVO> goodsCategories(Long id) {
        requireEnabledShop(id);
        return goodsCategoryMapper.selectList(Wrappers.<GoodsCategory>lambdaQuery()
                        .eq(GoodsCategory::getShopId, id).eq(GoodsCategory::getStatus, 1)
                        .orderByAsc(GoodsCategory::getSortOrder).orderByAsc(GoodsCategory::getId))
                .stream().map(category -> GoodsCategoryVO.from(category,
                        goodsMapper.selectCount(Wrappers.<Goods>lambdaQuery().eq(Goods::getCategoryId, category.getId()))))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<CommentVO> comments(Long id, PageQuery query) {
        requireEnabledShop(id);
        IPage<Comment> page = commentMapper.selectPage(new Page<Comment>(query.getPage(), query.getSize()),
                Wrappers.<Comment>lambdaQuery().eq(Comment::getShopId, id).eq(Comment::getStatus, 1)
                        .orderByDesc(Comment::getCreateTime));
        return pageResult(page, CommentVO::fromComment);
    }

    @Override
    public PageResult<AdminShopVO> adminPage(AdminShopQuery query) {
        IPage<Shop> page = shopMapper.selectPage(new Page<Shop>(query.getPage(), query.getSize()),
                Wrappers.<Shop>lambdaQuery()
                        .eq(query.getCategoryId() != null, Shop::getCategoryId, query.getCategoryId())
                        .eq(query.getStatus() != null, Shop::getStatus, query.getStatus())
                        .like(query.getKeyword() != null && !query.getKeyword().trim().isEmpty(), Shop::getName, query.getKeyword())
                        .orderByAsc(Shop::getSortOrder)
                        .orderByDesc(Shop::getId));
        Map<Long, String> categoryNames = categoryNames(page.getRecords());
        List<AdminShopVO> records = page.getRecords().stream()
                .map(shop -> AdminShopVO.fromShop(shop, categoryNames.get(shop.getCategoryId())))
                .collect(Collectors.toList());
        return new PageResult<AdminShopVO>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdminShopVO adminDetail(Long id) {
        Shop shop = requireShop(id);
        return AdminShopVO.fromShop(shop, categoryName(shop.getCategoryId()));
    }

    @Override
    @Transactional
    public AdminShopVO createAdminShop(AdminShopSaveRequest request) {
        Category category = requireEnabledCategory(request.getCategoryId());
        Shop shop = new Shop();
        applyRequest(shop, request);
        if (shopMapper.insert(shop) != 1) {
            throw new BusinessException("店铺保存失败，请稍后重试");
        }
        invalidateShopCache(shop.getId());
        return AdminShopVO.fromShop(shop, category.getName());
    }

    @Override
    @Transactional
    public AdminShopVO updateAdminShop(Long id, AdminShopSaveRequest request) {
        Shop shop = requireShop(id);
        Category category = requireEnabledCategory(request.getCategoryId());
        applyRequest(shop, request);
        if (shopMapper.updateById(shop) != 1) {
            throw new BusinessException("店铺保存失败，请稍后重试");
        }
        invalidateShopCache(id);
        return AdminShopVO.fromShop(shop, category.getName());
    }

    @Override
    @Transactional
    public void updateAdminShopStatus(Long id, AdminShopStatusRequest request) {
        Shop shop = requireShop(id);
        shop.setStatus(request.getStatus());
        if (shopMapper.updateById(shop) != 1) {
            throw new BusinessException("店铺状态保存失败，请稍后重试");
        }
        invalidateShopCache(id);
    }

    @Override
    @Transactional
    public AdminShopVO uploadAdminShopCover(Long id, MultipartFile file) {
        Shop shop = requireShop(id);
        ValidatedCover image = validateCover(file);
        LocalDate today = LocalDate.now();
        String objectKey = "qinghe-life-service/shops/" + today.getYear() + "/"
                + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + ".webp";
        String uploadedUrl;
        try {
            uploadedUrl = ossOperator.upload(objectKey, new ByteArrayInputStream(image.bytes), image.bytes.length, image.contentType);
        } catch (Exception exception) {
            log.error("店铺封面上传失败，shopId={}，type={}", id, exception.getClass().getSimpleName());
            throw new BusinessException(503, "店铺封面上传服务暂不可用");
        }

        String oldCoverUrl = shop.getCoverImage();
        try {
            shop.setCoverImage(uploadedUrl);
            if (shopMapper.updateById(shop) != 1) {
                throw new BusinessException("店铺封面保存失败，请稍后重试");
            }
        } catch (Exception exception) {
            safelyDeleteNewCover(objectKey);
            if (exception instanceof BusinessException) {
                throw (BusinessException) exception;
            }
            log.error("店铺封面保存失败，shopId={}，type={}", id, exception.getClass().getSimpleName());
            throw new BusinessException(503, "店铺封面保存失败，请稍后重试");
        }
        invalidateShopCache(id);
        safelyDeleteOldCover(oldCoverUrl);
        return AdminShopVO.fromShop(shop, categoryName(shop.getCategoryId()));
    }

    private Shop requireEnabledShop(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BusinessException(404, "商铺不存在或不可用");
        }
        return shop;
    }

    private Shop requireShop(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(404, "店铺不存在");
        }
        return shop;
    }

    private Category requireEnabledCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException("店铺分类不存在或已停用");
        }
        return category;
    }

    private void applyRequest(Shop shop, AdminShopSaveRequest request) {
        shop.setName(request.getName().trim());
        shop.setCategoryId(request.getCategoryId());
        shop.setAddress(request.getAddress().trim());
        shop.setPhone(request.getPhone() == null || request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
        shop.setScore(request.getScore().setScale(2, BigDecimal.ROUND_HALF_UP));
        shop.setStatus(request.getStatus());
        shop.setIsFeatured(request.getIsFeatured());
        shop.setSortOrder(request.getSortOrder());
    }

    private Map<Long, String> categoryNames(List<Shop> shops) {
        Map<Long, String> result = new HashMap<Long, String>();
        if (shops == null || shops.isEmpty()) {
            return result;
        }
        List<Long> ids = shops.stream().map(Shop::getCategoryId).filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return result;
        }
        for (Category category : categoryMapper.selectBatchIds(ids)) {
            result.put(category.getId(), category.getName());
        }
        return result;
    }

    private Map<Long, String> goodsCategoryNames(List<Goods> goods) {
        Map<Long, String> result = new HashMap<Long, String>();
        List<Long> ids = goods.stream().map(Goods::getCategoryId).filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return result;
        for (GoodsCategory category : goodsCategoryMapper.selectBatchIds(ids)) result.put(category.getId(), category.getName());
        return result;
    }

    private String categoryName(Long categoryId) {
        Category category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private void invalidateShopCache(Long shopId) {
        List<String> keys = new ArrayList<String>();
        keys.add(RedisKeys.shopDetail(shopId));
        keys.add(RedisKeys.shopGoods(shopId));
        if (goodsMapper != null) {
            for (Goods goods : goodsMapper.selectList(Wrappers.<Goods>lambdaQuery().eq(Goods::getShopId, shopId))) {
                keys.add(RedisKeys.goodsDetail(goods.getId()));
            }
        }
        catalogCache.evictAfterCommit(keys);
    }

    private ValidatedCover validateCover(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("请选择有效店铺封面");
        }
        if (file.getSize() > MAX_COVER_BYTES) {
            throw new BusinessException("店铺封面原文件不能超过3MB");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp"))) {
            throw new BusinessException("仅支持 jpg、jpeg、png 或 webp 图片");
        }
        try {
            byte[] bytes = file.getBytes();
            String detectedType = detectImageType(bytes);
            if (detectedType == null || !detectedType.equals(contentType) || !matchesExtension(fileName, detectedType)) {
                throw new BusinessException("图片扩展名、Content-Type 或文件内容不匹配");
            }
            int[] dimensions = "image/webp".equals(detectedType) ? readWebpDimensions(bytes) : readRasterDimensions(bytes);
            if (dimensions == null || dimensions[0] <= 0 || dimensions[1] <= 0) {
                throw new BusinessException("图片内容无效，请重新选择");
            }
            if (dimensions[0] > MAX_COVER_WIDTH || dimensions[1] > MAX_COVER_HEIGHT) {
                throw new BusinessException("店铺封面最大尺寸为1200×800");
            }
            return new ValidatedCover(bytes, detectedType);
        } catch (IOException exception) {
            throw new BusinessException("图片读取失败，请重新选择");
        }
    }

    private boolean matchesExtension(String fileName, String contentType) {
        return (fileName.endsWith(".png") && "image/png".equals(contentType))
                || ((fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) && "image/jpeg".equals(contentType))
                || (fileName.endsWith(".webp") && "image/webp".equals(contentType));
    }

    private String detectImageType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private int[] readRasterDimensions(byte[] bytes) throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
        if (input == null) return null;
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[] {reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } finally {
            input.close();
        }
    }

    private int[] readWebpDimensions(byte[] bytes) {
        if (bytes.length < 30) return null;
        String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if ("VP8X".equals(chunk)) {
            return new int[] {readLe24(bytes, 24) + 1, readLe24(bytes, 27) + 1};
        }
        if ("VP8 ".equals(chunk) && bytes.length >= 30 && bytes[23] == (byte) 0x9d && bytes[24] == 0x01 && bytes[25] == 0x2a) {
            return new int[] {readLe16(bytes, 26) & 0x3fff, readLe16(bytes, 28) & 0x3fff};
        }
        if ("VP8L".equals(chunk) && bytes.length >= 25 && bytes[20] == 0x2f) {
            long bits = (bytes[21] & 0xffL) | ((bytes[22] & 0xffL) << 8) | ((bytes[23] & 0xffL) << 16) | ((bytes[24] & 0xffL) << 24);
            return new int[] {(int) (bits & 0x3fff) + 1, (int) ((bits >> 14) & 0x3fff) + 1};
        }
        return null;
    }

    private int readLe16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    private int readLe24(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16);
    }

    private void safelyDeleteNewCover(String objectKey) {
        try {
            ossOperator.deleteObject(objectKey);
        } catch (Exception exception) {
            log.warn("新店铺封面补偿删除失败，type={}", exception.getClass().getSimpleName());
        }
    }

    private void safelyDeleteOldCover(String oldCoverUrl) {
        String oldKey = ossOperator.ownShopCoverKey(oldCoverUrl);
        if (oldKey == null) return;
        try {
            ossOperator.deleteObject(oldKey);
        } catch (Exception exception) {
            log.warn("旧店铺封面清理失败，type={}", exception.getClass().getSimpleName());
        }
    }

    private static class ValidatedCover {
        private final byte[] bytes;
        private final String contentType;
        private ValidatedCover(byte[] bytes, String contentType) { this.bytes = bytes; this.contentType = contentType; }
    }

    private ShopVO cachedDetail(Long id) {
        ShopVO result = catalogCache.getObject(RedisKeys.shopDetail(id), RedisKeys.shopLock(id), ShopVO.class,
                cacheProperties.getShopTtlMinutes(), () -> {
                    Shop shop = shopMapper.selectById(id);
                    return shop == null || !Integer.valueOf(1).equals(shop.getStatus()) ? null : ShopVO.fromShop(shop);
                });
        if (result == null) {
            throw new BusinessException(404, "商铺不存在或不可用");
        }
        return result;
    }

    private List<CachedGoods> loadShopGoods(Long shopId) {
        List<Goods> goods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery().eq(Goods::getShopId, shopId)
                .eq(Goods::getSaleStatus, "ON_SALE").orderByDesc(Goods::getSalesCount).orderByDesc(Goods::getId));
        Map<Long, String> names = goodsCategoryNames(goods);
        return goods.stream().map(item -> CachedGoods.from(item, names.get(item.getCategoryId()))).collect(Collectors.toList());
    }

    private <T, R> PageResult<R> pageResult(IPage<T> page, java.util.function.Function<T, R> converter) {
        List<R> records = page.getRecords().stream().map(converter).collect(Collectors.toList());
        return new PageResult<R>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
