package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 换物申请视图对象：一方用自己的物品向另一方的物品发起交换请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarterRequestVO {

    /** 换物申请 ID */
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

    /** 差价，正数表示发起方需补给接收方，负数表示接收方补给发起方，null 表示不涉及差价 */
    private BigDecimal priceDiff;

    /** 申请状态：PENDING / ACCEPTED / REJECTED / DONE / CANCELLED */
    private String status;

    /** 创建时间（字符串形式） */
    private String createdAt;
}
