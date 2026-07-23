package com.qinghe.life.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class BatchOperationRequest {
    @NotEmpty(message = "请先选择处理对象") @Size(max = 100, message = "单次最多处理100条")
    private List<Long> ids;
    @NotBlank(message = "请填写办理原因") @Size(max = 255, message = "办理原因不能超过255个字符")
    private String reason;
    @NotBlank(message = "请先重新预览后确认")
    private String previewToken;
}
