package com.qinghe.life.vo;

import lombok.Data;

/** Safe social-profile projection. It intentionally excludes phone, student, address and token data. */
@Data
public class PublicUserSummaryVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private Boolean followedByMe;
    private Long followerCount;
}
