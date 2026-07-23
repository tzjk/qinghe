package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qh_comment")
public class Comment extends BaseEntity {
    private Long userId;
    private Long shopId;
    private Long orderId;
    private Long blogId;
    private String commentType;
    private String content;
    private Integer score;
    private String images;
    private Long parentId;
    private Integer status;
}
