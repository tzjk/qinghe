package com.qinghe.life.dto;
import lombok.Data; import javax.validation.constraints.NotNull; import javax.validation.constraints.NotBlank; import javax.validation.constraints.Size;
@Data public class DormTransferRequest { @NotNull(message="请选择目标床位") private Long targetBedId; @NotBlank(message="换寝原因不能为空") @Size(max=255,message="换寝原因不能超过255个字符") private String reason; }
