package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    @Select({"<script>",
            "SELECT id, order_id, goods_id, goods_name, goods_image, goods_price, quantity, subtotal, create_time, update_time",
            "FROM qh_order_item WHERE order_id IN",
            "<foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>#{orderId}</foreach>",
            "ORDER BY order_id ASC, id ASC",
            "</script>"})
    List<OrderItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
