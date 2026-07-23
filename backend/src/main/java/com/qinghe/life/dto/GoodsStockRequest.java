package com.qinghe.life.dto;
import javax.validation.constraints.*; import lombok.Data;
@Data public class GoodsStockRequest { @NotNull @Min(0) private Integer stock; }
