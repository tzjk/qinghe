package com.qinghe.life.dto;
import java.math.BigDecimal; import javax.validation.constraints.*; import lombok.Data;
@Data public class AdminGoodsSaveRequest { @NotNull private Long shopId; private Long categoryId; @NotBlank @Size(max=100) private String name; @Size(max=500) private String description; @NotNull @DecimalMin("0.00") private BigDecimal price; @NotNull @Min(0) private Integer stock; @NotBlank @Pattern(regexp="ON_SALE|OFF_SALE") private String saleStatus; }
