package com.qinghe.life.dto;
import lombok.Data;
@Data public class AdminGoodsQuery extends PageQuery { private String keyword; private Long shopId; private Long shopCategoryId; private Long goodsCategoryId; private String saleStatus; }
