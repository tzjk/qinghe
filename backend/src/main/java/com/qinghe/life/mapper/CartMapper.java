package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.Cart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Update("UPDATE qh_cart c INNER JOIN qh_goods g ON c.goods_id = g.id "
            + "INNER JOIN qh_shop s ON g.shop_id = s.id "
            + "SET c.quantity = c.quantity + #{quantity}, c.shop_id = g.shop_id "
            + "WHERE c.user_id = #{userId} AND c.goods_id = #{goodsId} "
            + "AND g.sale_status = 'ON_SALE' AND s.status = 1 "
            + "AND c.quantity + #{quantity} <= g.stock")
    int increaseQuantityWithinCurrentStock(@Param("userId") Long userId,
                                           @Param("goodsId") Long goodsId,
                                           @Param("quantity") Integer quantity);

    @Insert("INSERT INTO qh_cart (user_id, shop_id, goods_id, quantity, selected) "
            + "SELECT #{userId}, g.shop_id, g.id, #{quantity}, 1 "
            + "FROM qh_goods g INNER JOIN qh_shop s ON g.shop_id = s.id "
            + "WHERE g.id = #{goodsId} AND g.sale_status = 'ON_SALE' "
            + "AND s.status = 1 AND #{quantity} <= g.stock")
    int insertFromCurrentGoods(@Param("userId") Long userId,
                               @Param("goodsId") Long goodsId,
                               @Param("quantity") Integer quantity);
}
