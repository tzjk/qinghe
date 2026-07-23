package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminShopQuery;
import com.qinghe.life.dto.AdminShopSaveRequest;
import com.qinghe.life.dto.AdminShopStatusRequest;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.dto.ShopQuery;
import com.qinghe.life.dto.ShopGoodsQuery;
import com.qinghe.life.vo.AdminShopVO;
import com.qinghe.life.vo.CommentVO;
import com.qinghe.life.vo.GoodsVO;
import com.qinghe.life.vo.GoodsCategoryVO;
import com.qinghe.life.vo.ShopVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ShopService {
    PageResult<ShopVO> page(ShopQuery query);
    ShopVO detail(Long id);
    PageResult<GoodsVO> goods(Long id, ShopGoodsQuery query);
    List<GoodsCategoryVO> goodsCategories(Long id);
    PageResult<CommentVO> comments(Long id, PageQuery query);
    PageResult<AdminShopVO> adminPage(AdminShopQuery query);
    AdminShopVO adminDetail(Long id);
    AdminShopVO createAdminShop(AdminShopSaveRequest request);
    AdminShopVO updateAdminShop(Long id, AdminShopSaveRequest request);
    void updateAdminShopStatus(Long id, AdminShopStatusRequest request);
    AdminShopVO uploadAdminShopCover(Long id, MultipartFile file);
}
