package com.qinghe.life.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminGoodsQuery;
import com.qinghe.life.dto.AdminGoodsSaveRequest;
import com.qinghe.life.dto.GoodsStatusRequest;
import com.qinghe.life.dto.GoodsStockRequest;
import com.qinghe.life.entity.Goods;
import com.qinghe.life.entity.GoodsCategory;
import com.qinghe.life.entity.Shop;
import com.qinghe.life.exception.BusinessException;
import com.qinghe.life.mapper.GoodsCategoryMapper;
import com.qinghe.life.mapper.GoodsMapper;
import com.qinghe.life.mapper.ShopMapper;
import com.qinghe.life.oss.AliyunOSSOperator;
import com.qinghe.life.service.AdminGoodsService;
import com.qinghe.life.vo.AdminGoodsVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminGoodsServiceImpl implements AdminGoodsService {
    private static final Logger log = LoggerFactory.getLogger(AdminGoodsServiceImpl.class);
    private static final long MAX_IMAGE_BYTES = 3L * 1024L * 1024L;
    private static final int MAX_IMAGE_WIDTH = 800;
    private static final int MAX_IMAGE_HEIGHT = 800;
    private static final String ON_SALE = "ON_SALE";
    private static final String OFF_SALE = "OFF_SALE";

    private final GoodsMapper goodsMapper;
    private final ShopMapper shopMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final AliyunOSSOperator ossOperator;

    @Autowired
    public AdminGoodsServiceImpl(GoodsMapper goodsMapper, ShopMapper shopMapper, GoodsCategoryMapper goodsCategoryMapper,
                                 AliyunOSSOperator ossOperator) {
        this.goodsMapper = goodsMapper;
        this.shopMapper = shopMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
        this.ossOperator = ossOperator;
    }

    public AdminGoodsServiceImpl(GoodsMapper goodsMapper, ShopMapper shopMapper, AliyunOSSOperator ossOperator) {
        this(goodsMapper, shopMapper, null, ossOperator);
    }

    @Override
    public PageResult<AdminGoodsVO> page(AdminGoodsQuery query) {
        List<Long> categoryShopIds = categoryShopIds(query.getShopCategoryId());
        if (query.getShopCategoryId() != null && categoryShopIds.isEmpty()) {
            return new PageResult<AdminGoodsVO>(new ArrayList<AdminGoodsVO>(), 0L, query.getPage(), query.getSize());
        }
        IPage<Goods> page = goodsMapper.selectPage(new Page<Goods>(query.getPage(), query.getSize()),
                Wrappers.<Goods>lambdaQuery()
                        .eq(query.getShopId() != null, Goods::getShopId, query.getShopId())
                        .in(query.getShopCategoryId() != null, Goods::getShopId, categoryShopIds)
                        .eq(query.getGoodsCategoryId() != null, Goods::getCategoryId, query.getGoodsCategoryId())
                        .eq(query.getSaleStatus() != null && !query.getSaleStatus().trim().isEmpty(), Goods::getSaleStatus, query.getSaleStatus())
                        .like(query.getKeyword() != null && !query.getKeyword().trim().isEmpty(), Goods::getName, query.getKeyword().trim())
                        .orderByDesc(Goods::getId));
        return toPage(page);
    }

    @Override
    public AdminGoodsVO detail(Long id) {
        return toAdminGoodsVO(requireGoods(id));
    }

    @Override
    public AdminGoodsVO create(AdminGoodsSaveRequest request) {
        requireEnabledShop(request.getShopId());
        Goods goods = new Goods();
        applyRequest(goods, request, true);
        goods.setSalesCount(0);
        if (goodsMapper.insert(goods) != 1) {
            throw new BusinessException("商品保存失败，请稍后重试");
        }
        return toAdminGoodsVO(goods);
    }

    @Override
    @Transactional
    public AdminGoodsVO update(Long id, AdminGoodsSaveRequest request) {
        Goods goods = requireGoods(id);
        Long originalCategoryId = goods.getCategoryId();
        requireEnabledShop(request.getShopId());
        applyRequest(goods, request, false);
        if (goodsMapper.updateById(goods) != 1) {
            throw new BusinessException("商品保存失败，请稍后重试");
        }
        if (request.getCategoryId() == null && originalCategoryId != null
                && goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                .eq(Goods::getId, id).set(Goods::getCategoryId, null)) != 1) {
            throw new BusinessException("商品分类清空失败，请稍后重试");
        }
        return toAdminGoodsVO(goods);
    }

    @Override
    public void status(Long id, GoodsStatusRequest request) {
        Goods goods = requireGoods(id);
        goods.setSaleStatus(request.getSaleStatus());
        if (goodsMapper.updateById(goods) != 1) {
            throw new BusinessException("商品状态保存失败，请稍后重试");
        }
    }

    @Override
    public void stock(Long id, GoodsStockRequest request) {
        Goods goods = requireGoods(id);
        goods.setStock(request.getStock());
        if (goodsMapper.updateById(goods) != 1) {
            throw new BusinessException("商品库存保存失败，请稍后重试");
        }
    }

    @Override
    public AdminGoodsVO image(Long id, MultipartFile file) {
        Goods goods = requireGoods(id);
        ValidatedImage image = validateImage(file);
        LocalDate today = LocalDate.now();
        String objectKey = "qinghe-life-service/goods/" + today.getYear() + "/"
                + String.format("%02d", today.getMonthValue()) + "/" + UUID.randomUUID() + ".webp";
        String uploadedUrl;
        try {
            uploadedUrl = ossOperator.upload(objectKey, new ByteArrayInputStream(image.bytes), image.bytes.length, image.contentType);
        } catch (Exception exception) {
            log.error("商品主图上传失败，goodsId={}，type={}", id, exception.getClass().getSimpleName());
            throw new BusinessException(503, "商品主图上传服务暂不可用");
        }

        String oldImageUrl = goods.getCoverImage();
        try {
            goods.setCoverImage(uploadedUrl);
            if (goodsMapper.updateById(goods) != 1) {
                throw new BusinessException("商品主图保存失败，请稍后重试");
            }
        } catch (Exception exception) {
            safelyDeleteNewImage(objectKey);
            if (exception instanceof BusinessException) {
                throw (BusinessException) exception;
            }
            log.error("商品主图保存失败，goodsId={}，type={}", id, exception.getClass().getSimpleName());
            throw new BusinessException(503, "商品主图保存失败，请稍后重试");
        }
        safelyDeleteOldImage(oldImageUrl);
        return toAdminGoodsVO(goods);
    }

    private PageResult<AdminGoodsVO> toPage(IPage<Goods> page) {
        Map<Long, Shop> shops = shopsById(page.getRecords());
        Map<Long, GoodsCategory> categories = categoriesById(page.getRecords());
        List<AdminGoodsVO> records = new ArrayList<AdminGoodsVO>();
        for (Goods goods : page.getRecords()) {
            Shop shop = shops.get(goods.getShopId());
            records.add(AdminGoodsVO.from(goods, shop == null ? null : shop.getName(),
                    shop == null ? null : shop.getCategoryId(), categories.get(goods.getCategoryId())));
        }
        return new PageResult<AdminGoodsVO>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private AdminGoodsVO toAdminGoodsVO(Goods goods) {
        Shop shop = goods.getShopId() == null ? null : shopMapper.selectById(goods.getShopId());
        GoodsCategory category = goods.getCategoryId() == null || goodsCategoryMapper == null ? null
                : goodsCategoryMapper.selectById(goods.getCategoryId());
        return AdminGoodsVO.from(goods, shop == null ? null : shop.getName(), shop == null ? null : shop.getCategoryId(), category);
    }

    private List<Long> categoryShopIds(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<Long>();
        }
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().eq(Shop::getCategoryId, categoryId));
        List<Long> ids = new ArrayList<Long>();
        for (Shop shop : shops) {
            ids.add(shop.getId());
        }
        return ids;
    }

    private Map<Long, Shop> shopsById(List<Goods> goods) {
        Map<Long, Shop> result = new HashMap<Long, Shop>();
        List<Long> shopIds = new ArrayList<Long>();
        for (Goods item : goods) {
            if (item.getShopId() != null && !shopIds.contains(item.getShopId())) {
                shopIds.add(item.getShopId());
            }
        }
        if (!shopIds.isEmpty()) {
            for (Shop shop : shopMapper.selectBatchIds(shopIds)) {
                result.put(shop.getId(), shop);
            }
        }
        return result;
    }

    private Map<Long, GoodsCategory> categoriesById(List<Goods> goods) {
        Map<Long, GoodsCategory> result = new HashMap<Long, GoodsCategory>();
        if (goodsCategoryMapper == null) return result;
        List<Long> ids = new ArrayList<Long>();
        for (Goods item : goods) {
            if (item.getCategoryId() != null && !ids.contains(item.getCategoryId())) ids.add(item.getCategoryId());
        }
        if (!ids.isEmpty()) {
            for (GoodsCategory category : goodsCategoryMapper.selectBatchIds(ids)) result.put(category.getId(), category);
        }
        return result;
    }

    private Goods requireGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return goods;
    }

    private Shop requireEnabledShop(Long id) {
        Shop shop = id == null ? null : shopMapper.selectById(id);
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BusinessException("所属店铺不存在或已停用");
        }
        return shop;
    }

    private void applyRequest(Goods goods, AdminGoodsSaveRequest request, boolean creating) {
        goods.setShopId(request.getShopId());
        if (request.getCategoryId() == null) {
            goods.setCategoryId(null);
        } else {
            GoodsCategory category = requireMatchingCategory(request.getCategoryId(), request.getShopId());
            boolean retainDisabledCategory = !creating && request.getCategoryId().equals(goods.getCategoryId());
            if (!Integer.valueOf(1).equals(category.getStatus()) && !retainDisabledCategory) {
                throw new BusinessException("商品分类已停用，请选择启用分类");
            }
            goods.setCategoryId(category.getId());
        }
        goods.setName(request.getName().trim());
        goods.setDescription(request.getDescription() == null || request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
        goods.setPrice(request.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP));
        goods.setStock(request.getStock());
        goods.setSaleStatus(request.getSaleStatus());
    }

    private GoodsCategory requireMatchingCategory(Long categoryId, Long shopId) {
        GoodsCategory category = goodsCategoryMapper == null ? null : goodsCategoryMapper.selectById(categoryId);
        if (category == null) throw new BusinessException("商品分类不存在");
        if (!shopId.equals(category.getShopId())) throw new BusinessException("商品分类不属于所选店铺，请重新选择");
        return category;
    }

    private ValidatedImage validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("请选择有效商品主图");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("商品主图原文件不能超过3MB");
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
            if (dimensions[0] > MAX_IMAGE_WIDTH || dimensions[1] > MAX_IMAGE_HEIGHT) {
                throw new BusinessException("商品主图最大尺寸为800×800");
            }
            return new ValidatedImage(bytes, detectedType);
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
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
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
        String chunk = new String(bytes, 12, 4, StandardCharsets.US_ASCII);
        if ("VP8X".equals(chunk)) return new int[] {readLe24(bytes, 24) + 1, readLe24(bytes, 27) + 1};
        if ("VP8 ".equals(chunk) && bytes[23] == (byte) 0x9d && bytes[24] == 0x01 && bytes[25] == 0x2a) return new int[] {readLe16(bytes, 26) & 0x3fff, readLe16(bytes, 28) & 0x3fff};
        if ("VP8L".equals(chunk) && bytes.length >= 25 && bytes[20] == 0x2f) {
            long bits = (bytes[21] & 0xffL) | ((bytes[22] & 0xffL) << 8) | ((bytes[23] & 0xffL) << 16) | ((bytes[24] & 0xffL) << 24);
            return new int[] {(int) (bits & 0x3fff) + 1, (int) ((bits >> 14) & 0x3fff) + 1};
        }
        return null;
    }

    private int readLe16(byte[] bytes, int offset) { return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8); }
    private int readLe24(byte[] bytes, int offset) { return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16); }

    private void safelyDeleteNewImage(String objectKey) {
        try { ossOperator.deleteObject(objectKey); }
        catch (Exception exception) { log.warn("新商品主图补偿删除失败，type={}", exception.getClass().getSimpleName()); }
    }

    private void safelyDeleteOldImage(String oldImageUrl) {
        String oldKey = ossOperator.ownGoodsImageKey(oldImageUrl);
        if (oldKey == null) return;
        try { ossOperator.deleteObject(oldKey); }
        catch (Exception exception) { log.warn("旧商品主图清理失败，type={}", exception.getClass().getSimpleName()); }
    }

    private static class ValidatedImage {
        private final byte[] bytes;
        private final String contentType;
        private ValidatedImage(byte[] bytes, String contentType) { this.bytes = bytes; this.contentType = contentType; }
    }
}
