package com.qinghe.life.service;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.dto.AdminExplorePostQuery;
import com.qinghe.life.dto.ExploreCommentCreateRequest;
import com.qinghe.life.dto.ExplorePostQuery;
import com.qinghe.life.dto.ExplorePostSaveRequest;
import com.qinghe.life.dto.ExploreStatusRequest;
import com.qinghe.life.dto.NearbyShopQuery;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.vo.ExploreCommentVO;
import com.qinghe.life.vo.ExploreInteractionVO;
import com.qinghe.life.vo.ExplorePostVO;
import com.qinghe.life.vo.NearbyShopVO;
import com.qinghe.life.vo.FollowingFeedVO;
import org.springframework.web.multipart.MultipartFile;

public interface ExploreService {
    PageResult<ExplorePostVO> page(ExplorePostQuery query);
    ExplorePostVO detail(Long id);
    ExplorePostVO create(ExplorePostSaveRequest request);
    String uploadImage(MultipartFile file);
    ExplorePostVO update(Long id, ExplorePostSaveRequest request);
    void delete(Long id);
    ExploreInteractionVO like(Long id);
    ExploreInteractionVO unlike(Long id);
    PageResult<ExploreCommentVO> comments(Long id, PageQuery query);
    ExploreCommentVO comment(Long id, ExploreCommentCreateRequest request);
    PageResult<NearbyShopVO> nearby(NearbyShopQuery query);
    FollowingFeedVO followingFeed(Long maxTime, Long offset, Integer size);
    PageResult<ExplorePostVO> adminPage(AdminExplorePostQuery query);
    ExplorePostVO adminDetail(Long id);
    PageResult<ExploreCommentVO> adminComments(Long postId, PageQuery query);
    void updatePostStatus(Long id, ExploreStatusRequest request);
    void updateCommentStatus(Long id, ExploreStatusRequest request);
}
