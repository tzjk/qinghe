package com.qinghe.life.controller;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.service.FollowService;
import com.qinghe.life.utils.UserContext;
import com.qinghe.life.vo.FollowCountsVO;
import com.qinghe.life.vo.PublicUserSummaryVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowService followService;
    public FollowController(FollowService followService) { this.followService = followService; }
    @PostMapping("/{targetUserId}") public Result<FollowCountsVO> follow(@PathVariable Long targetUserId) { return Result.success(followService.follow(targetUserId)); }
    @DeleteMapping("/{targetUserId}") public Result<FollowCountsVO> unfollow(@PathVariable Long targetUserId) { return Result.success(followService.unfollow(targetUserId)); }
    @GetMapping("/{targetUserId}/status") public Result<FollowCountsVO> status(@PathVariable Long targetUserId) { return Result.success(followService.relation(targetUserId)); }
    @GetMapping("/me/following") public Result<PageResult<PublicUserSummaryVO>> mineFollowing(@Valid PageQuery query) { return Result.success(followService.following(requireUser(), query.getPage(), query.getSize())); }
    @GetMapping("/me/followers") public Result<PageResult<PublicUserSummaryVO>> mineFollowers(@Valid PageQuery query) { return Result.success(followService.followers(requireUser(), query.getPage(), query.getSize())); }
    @GetMapping("/users/{userId}/following") public Result<PageResult<PublicUserSummaryVO>> following(@PathVariable Long userId, @Valid PageQuery query) { return Result.success(followService.following(userId, query.getPage(), query.getSize())); }
    @GetMapping("/users/{userId}/followers") public Result<PageResult<PublicUserSummaryVO>> followers(@PathVariable Long userId, @Valid PageQuery query) { return Result.success(followService.followers(userId, query.getPage(), query.getSize())); }
    @GetMapping("/{targetUserId}/common") public Result<List<PublicUserSummaryVO>> common(@PathVariable Long targetUserId) { return Result.success(followService.common(targetUserId)); }
    private Long requireUser() { Long id = UserContext.getUserId(); if (id == null) throw new com.qinghe.life.exception.BusinessException(401, "请先登录"); return id; }
}
