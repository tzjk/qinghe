package com.qinghe.life.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class BatchPreviewRequest {
    @NotEmpty(message = "请先选择处理对象") @Size(max = 100, message = "单次最多预览100条")
    private List<Long> ids;
}
