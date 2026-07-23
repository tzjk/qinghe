package com.qinghe.life.dto;
import javax.validation.constraints.Pattern; import lombok.Data;
@Data public class GoodsStatusRequest { @Pattern(regexp="ON_SALE|OFF_SALE") private String saleStatus; }
