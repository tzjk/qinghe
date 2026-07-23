package com.qinghe.life.dto;
import lombok.Data; import javax.validation.constraints.NotBlank; import javax.validation.constraints.Size;
@Data public class DormCheckoutRequest { @NotBlank(message="退宿原因不能为空") @Size(max=255,message="退宿原因不能超过255个字符") private String reason; }
