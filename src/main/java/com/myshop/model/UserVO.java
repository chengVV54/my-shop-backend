package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户视图对象：对外输出用户信息，<b>不包含密码哈希</b>。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 头像地址，可为空 */
    private String avatar;

    /** 积分 */
    private Integer credit;

    /** 注册时间（字符串形式） */
    private String createdAt;
}
