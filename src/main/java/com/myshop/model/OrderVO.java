package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 二手订单视图对象：买家直接购买卖家物品生成的订单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /** 订单 ID */
    private Long id;

    /** 买家用户 ID */
    private Long buyerId;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 购买的物品 ID */
    private Long itemId;

    /** 成交价格 */
    private BigDecimal price;

    /** 收货地址 */
    private String address;

    /** 订单状态：PENDING / PAID / SHIPPED / DONE / CANCELLED */
    private String status;

    /** 下单时间（字符串形式） */
    private String createdAt;
}
