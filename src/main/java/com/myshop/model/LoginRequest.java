package com.myshop.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体。
 */
@Data
public class LoginRequest {

    /** 用户名 */
    @NotBlank(message = "不能为空")
    private String username;

    /** 明文密码，服务端与库中 BCrypt 哈希比对 */
    @NotBlank(message = "不能为空")
    private String password;
}
