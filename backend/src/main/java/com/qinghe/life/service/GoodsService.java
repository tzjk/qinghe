package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.GoodsQuery;
import com.qinghe.life.vo.GoodsVO;

public interface GoodsService {
    PageResult<GoodsVO> page(GoodsQuery query);
    GoodsVO detail(Long id);
}
