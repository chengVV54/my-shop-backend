package com.myshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体，对应 {@code users} 表。
 *
 * <p>{@code password} 存的是 BCrypt 哈希（60 字符左右），绝不存明文；
 * 对外一律通过 {@code UserVO} 输出，避免哈希泄露。
 */
@Data
@TableName("users")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名，全局唯一 */
    private String username;

    /** BCrypt 密码哈希，禁止明文、禁止序列化给前端 */
    private String password;

    /** 头像地址，可为空 */
    private String avatar;

    /** 积分，注册默认 100 */
    private Integer credit;

    /** 注册时间 */
    private LocalDateTime createdAt;
}
