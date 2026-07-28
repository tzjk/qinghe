package com.qinghe.life.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExploreCommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private String commentStatus;
    private LocalDateTime createdAt;
}
