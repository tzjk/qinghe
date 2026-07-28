package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("qh_explore_post")
public class ExplorePost {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long shopId;
    private String title;
    private String content;
    private String images;
    private Integer likeCount;
    private Integer commentCount;
    private String postStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
