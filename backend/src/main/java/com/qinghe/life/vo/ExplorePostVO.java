package com.qinghe.life.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ExplorePostVO {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private Long shopId;
    private String shopName;
    private String title;
    private String content;
    private List<String> images;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean liked;
    private Boolean followedByMe;
    private List<PublicUserSummaryVO> topLikers;
    private String postStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
