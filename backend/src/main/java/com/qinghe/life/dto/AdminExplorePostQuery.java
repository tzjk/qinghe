package com.qinghe.life.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;

@Data
public class AdminExplorePostQuery {
    @Min(1) private Long page = 1L;
    @Min(1) @Max(50) private Long size = 10L;
    private Long shopId;
    private Long userId;
    private String status;
    private String keyword;
}
