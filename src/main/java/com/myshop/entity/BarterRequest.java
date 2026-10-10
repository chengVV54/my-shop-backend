package com.myshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 换物申请表实体，对应 {@code barter_requests} 表。
 */
@Data
@TableName("barter_requests")
public class BarterRequest {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起方用户 ID */
    private Long fromUserId;

    /** 接收方用户 ID */
    private Long toUserId;

    /** 发起方用来交换的物品 ID */
    private Long fromItemId;

    /** 接收方被交换的物品 ID */
    private Long toItemId;

    /** 申请留言 */
    private String message;

    /** 差价，null 表示不涉及差价 */
    private BigDecimal priceDiff;

    /** 申请状态：PENDING / ACCEPTED / REJECTED / DONE / CANCELLED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
