package com.myshop.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体。
 */
@Data
public class RegisterRequest {

    /** 用户名，3~20 位，全局唯一 */
    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 20, message = "长度需为 3~20 位")
    private String username;

    /** 明文密码，至少 6 位；服务端用 BCrypt 加密后存储，不落库明文 */
    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度需为 6~64 位")
    private String password;
}
