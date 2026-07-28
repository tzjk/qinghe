package com.qinghe.life.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ExploreCommentCreateRequest {
    @NotBlank(message = "评论内容不能为空") @Size(max = 500, message = "评论不能超过500个字符") private String content;
}
