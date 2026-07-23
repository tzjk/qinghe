package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_blog_like")
public class BlogLike extends BaseEntity {
    private Long userId;
    private Long blogId;
}
