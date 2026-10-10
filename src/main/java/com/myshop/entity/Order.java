package com.myshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手订单实体，对应 {@code orders} 表。
 */
@Data
@TableName("orders")
public class Order {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
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

    /** 下单时间 */
    private LocalDateTime createdAt;
}
