package com.qinghe.life.controller;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.dto.ShopQuery;
import com.qinghe.life.dto.ShopGoodsQuery;
import com.qinghe.life.service.ShopService;
import com.qinghe.life.vo.CommentVO;
import com.qinghe.life.vo.GoodsVO;
import com.qinghe.life.vo.GoodsCategoryVO;
import com.qinghe.life.vo.ShopVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/shops")
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public Result<PageResult<ShopVO>> page(@Valid ShopQuery query) {
        return Result.success(shopService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ShopVO> detail(@PathVariable Long id) {
        return Result.success(shopService.detail(id));
    }

    @GetMapping("/{id}/goods")
    public Result<PageResult<GoodsVO>> goods(@PathVariable Long id, @Valid ShopGoodsQuery query) {
        return Result.success(shopService.goods(id, query));
    }

    @GetMapping("/{id}/goods-categories")
    public Result<List<GoodsCategoryVO>> goodsCategories(@PathVariable Long id) {
        return Result.success(shopService.goodsCategories(id));
    }

    @GetMapping("/{id}/comments")
    public Result<PageResult<CommentVO>> comments(@PathVariable Long id, @Valid PageQuery query) {
        return Result.success(shopService.comments(id, query));
    }
}
