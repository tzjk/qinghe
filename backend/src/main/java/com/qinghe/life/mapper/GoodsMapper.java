package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
    @Update("UPDATE qh_goods SET stock = stock - #{quantity}, sales_count = sales_count + #{quantity} "
            + "WHERE id = #{goodsId} AND sale_status = 'ON_SALE' AND stock >= #{quantity}")
    int decreaseStockIfSaleable(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);

    @Update("UPDATE qh_goods SET stock = stock + #{quantity}, "
            + "sales_count = CASE WHEN sales_count >= #{quantity} THEN sales_count - #{quantity} ELSE 0 END "
            + "WHERE id = #{goodsId}")
    int restoreStockAfterOrderCancellation(@Param("goodsId") Long goodsId, @Param("quantity") Integer quantity);
}
