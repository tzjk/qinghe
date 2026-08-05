package com.qinghe.life.vo;

import java.util.List;
import lombok.Data;

@Data
public class FollowingFeedVO {
    private List<ExplorePostVO> records;
    private Long minTime;
    private Long offset;
    private Boolean hasMore;
}
