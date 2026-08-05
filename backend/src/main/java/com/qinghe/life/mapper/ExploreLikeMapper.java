package com.qinghe.life.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinghe.life.entity.ExploreLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExploreLikeMapper extends BaseMapper<ExploreLike> {
    @Insert("INSERT IGNORE INTO qh_explore_like (post_id, user_id, created_at) VALUES (#{postId}, #{userId}, NOW())")
    int insertIgnore(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM qh_explore_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);
}
