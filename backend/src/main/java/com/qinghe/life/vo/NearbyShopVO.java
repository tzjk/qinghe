package com.qinghe.life.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class NearbyShopVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String address;
    private String coverImage;
    private BigDecimal score;
    private BigDecimal distance;
}
