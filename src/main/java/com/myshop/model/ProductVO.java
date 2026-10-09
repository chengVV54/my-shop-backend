package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    /** 商品 ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 价格，以物换物类型为 0 */
    private BigDecimal price;

    /** 商品类型：NEW 新品 / SECOND_HAND 二手 / BARTER 以物换物 */
    private String type;
}
