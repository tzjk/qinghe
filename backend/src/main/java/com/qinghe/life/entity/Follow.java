package com.qinghe.life.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("qh_follow")
public class Follow {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long followUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
