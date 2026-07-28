package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("qh_explore_comment")
public class ExploreComment {
    @TableId(type = IdType.AUTO) private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private String commentStatus;
    private LocalDateTime createdAt;
}
