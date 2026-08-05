package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.vo.FollowCountsVO;
import com.qinghe.life.vo.PublicUserSummaryVO;
import java.util.List;

public interface FollowService {
    FollowCountsVO follow(Long targetUserId);
    FollowCountsVO unfollow(Long targetUserId);
    FollowCountsVO relation(Long targetUserId);
    PageResult<PublicUserSummaryVO> following(Long userId, long page, long size);
    PageResult<PublicUserSummaryVO> followers(Long userId, long page, long size);
    List<PublicUserSummaryVO> common(Long targetUserId);
    java.util.Set<Long> followingIds(Long userId);
    java.util.Set<Long> followerIds(Long userId);
}
