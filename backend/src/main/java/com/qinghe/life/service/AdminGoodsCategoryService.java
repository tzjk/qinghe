package com.qinghe.life.service;

import com.qinghe.life.dto.AdminGoodsCategoryQuery;
import com.qinghe.life.dto.GoodsCategorySaveRequest;
import com.qinghe.life.dto.GoodsCategoryStatusRequest;
import com.qinghe.life.dto.GoodsCategoryUpdateRequest;
import com.qinghe.life.vo.GoodsCategoryVO;
import java.util.List;

public interface AdminGoodsCategoryService {
    List<GoodsCategoryVO> list(AdminGoodsCategoryQuery query);
    GoodsCategoryVO create(GoodsCategorySaveRequest request);
    GoodsCategoryVO update(Long id, GoodsCategoryUpdateRequest request);
    void updateStatus(Long id, GoodsCategoryStatusRequest request);
}
