package com.qinghe.life.vo;

import lombok.Data;

@Data
public class FollowCountsVO {
    private Long userId;
    private Long followingCount;
    private Long followerCount;
    private Boolean followedByMe;
}
