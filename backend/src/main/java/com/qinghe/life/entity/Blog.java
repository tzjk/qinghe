package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_blog")
public class Blog extends BaseEntity {
    private Long userId;
    private Long shopId;
    private String title;
    private String content;
    private String coverImage;
    private Integer likeCount;
    private Integer favoriteCount;
    private String blogStatus;
}
