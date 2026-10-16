package com.myshop.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发起换物请求体（最小版本只支持 1:1 换物）。
 *
 * <p>只描述「换什么」：我方提供的物品 ID、想换取的对方物品 ID，以及可选留言。
 * 发起人身份刻意不放在请求体里，由服务端从 JWT 解析出的
 * {@code com.myshop.config.UserContext#getCurrentUserId()} 获取，
 * 避免前端伪造 {@code fromUserId} 冒充他人发起申请。
 */
@Data
public class CreateBarterRequest {

    /** 想换取的物品 ID（对方的物品），必填 */
    @NotNull(message = "不能为空")
    private Long toItemId;

    /** 用来交换的物品 ID（自己的物品），必填 */
    @NotNull(message = "不能为空")
    private Long fromItemId;

    /** 申请留言，最多 500 字，选填（对应 barter_requests.message VARCHAR(500)） */
    @Size(max = 500, message = "长度不能超过 500 字")
    private String message;
}
