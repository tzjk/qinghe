package com.qinghe.life.controller;

import com.qinghe.life.common.PageResult;
import com.qinghe.life.common.Result;
import com.qinghe.life.dto.ExploreCommentCreateRequest;
import com.qinghe.life.dto.ExplorePostQuery;
import com.qinghe.life.dto.ExplorePostSaveRequest;
import com.qinghe.life.dto.NearbyShopQuery;
import com.qinghe.life.dto.PageQuery;
import com.qinghe.life.service.ExploreService;
import com.qinghe.life.vo.ExploreCommentVO;
import com.qinghe.life.vo.ExploreInteractionVO;
import com.qinghe.life.vo.ExplorePostVO;
import com.qinghe.life.vo.NearbyShopVO;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/explore")
public class ExploreController {
    private final ExploreService exploreService;
    public ExploreController(ExploreService exploreService) { this.exploreService = exploreService; }
    @GetMapping("/posts") public Result<PageResult<ExplorePostVO>> page(@Valid ExplorePostQuery query) { return Result.success(exploreService.page(query)); }
    @GetMapping("/posts/{id}") public Result<ExplorePostVO> detail(@PathVariable Long id) { return Result.success(exploreService.detail(id)); }
    @PostMapping("/posts") public Result<ExplorePostVO> create(@Valid @RequestBody ExplorePostSaveRequest request) { return Result.success(exploreService.create(request)); }
    @PostMapping(value = "/images", consumes = "multipart/form-data") public Result<String> uploadImage(@RequestParam("file") MultipartFile file) { return Result.success(exploreService.uploadImage(file)); }
    @PutMapping("/posts/{id}") public Result<ExplorePostVO> update(@PathVariable Long id, @Valid @RequestBody ExplorePostSaveRequest request) { return Result.success(exploreService.update(id, request)); }
    @DeleteMapping("/posts/{id}") public Result<Void> delete(@PathVariable Long id) { exploreService.delete(id); return Result.success(); }
    @PostMapping("/posts/{id}/like") public Result<ExploreInteractionVO> like(@PathVariable Long id) { return Result.success(exploreService.like(id)); }
    @DeleteMapping("/posts/{id}/like") public Result<ExploreInteractionVO> unlike(@PathVariable Long id) { return Result.success(exploreService.unlike(id)); }
    @GetMapping("/posts/{id}/comments") public Result<PageResult<ExploreCommentVO>> comments(@PathVariable Long id, @Valid PageQuery query) { return Result.success(exploreService.comments(id, query)); }
    @PostMapping("/posts/{id}/comments") public Result<ExploreCommentVO> comment(@PathVariable Long id, @Valid @RequestBody ExploreCommentCreateRequest request) { return Result.success(exploreService.comment(id, request)); }
    @GetMapping("/shops/nearby") public Result<PageResult<NearbyShopVO>> nearby(@Valid NearbyShopQuery query) { return Result.success(exploreService.nearby(query)); }
}
