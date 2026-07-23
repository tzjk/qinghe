package com.qinghe.life.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Long page;
    private Long size;

    public static <T> PageResult<T> from(IPage<T> pageData) {
        return new PageResult<T>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    public static <T> PageResult<T> empty(Long page, Long size) {
        return new PageResult<T>(Collections.<T>emptyList(), 0L, page, size);
    }
}
