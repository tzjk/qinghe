package com.qinghe.life.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ExplorePostSaveRequest {
    @NotNull(message = "请选择关联店铺") private Long shopId;
    @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题不能超过100个字符") private String title;
    @NotBlank(message = "内容不能为空") @Size(max = 2000, message = "内容不能超过2000个字符") private String content;
    @Size(max = 9, message = "图片最多9张") private List<@NotBlank(message = "图片地址不能为空") @Size(max = 512, message = "图片地址过长") String> images;
}
