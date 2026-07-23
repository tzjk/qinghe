package com.qinghe.life.controller;

import com.qinghe.life.common.Result;
import com.qinghe.life.service.CategoryService;
import com.qinghe.life.vo.CategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<CategoryVO>> listEnabled() {
        return Result.success(categoryService.listEnabled());
    }
}
