package com.qinghe.life.controller;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.AdminExplorePostQuery;
import com.qinghe.life.dto.ExploreStatusRequest;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.service.ExploreService;
import com.qinghe.life.vo.ExploreCommentVO;
import com.qinghe.life.vo.ExplorePostVO;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/explore")
public class AdminExploreController {
    private final ExploreService exploreService;
    public AdminExploreController(ExploreService exploreService) { this.exploreService = exploreService; }
    @GetMapping("/posts") public Result<PageResult<ExplorePostVO>> page(@Valid AdminExplorePostQuery query) { return Result.success(exploreService.adminPage(query)); }
    @GetMapping("/posts/{id}") public Result<ExplorePostVO> detail(@PathVariable Long id) { return Result.success(exploreService.adminDetail(id)); }
    @GetMapping("/posts/{id}/comments") public Result<PageResult<ExploreCommentVO>> comments(@PathVariable Long id, @Valid PageQuery query) { return Result.success(exploreService.adminComments(id, query)); }
    @PutMapping("/posts/{id}/status") public Result<Void> postStatus(@PathVariable Long id, @Valid @RequestBody ExploreStatusRequest request) { exploreService.updatePostStatus(id, request); return Result.success(); }
    @PutMapping("/comments/{id}/status") public Result<Void> commentStatus(@PathVariable Long id, @Valid @RequestBody ExploreStatusRequest request) { exploreService.updateCommentStatus(id, request); return Result.success(); }
}
