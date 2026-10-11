package com.myshop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应：JWT 与用户信息。
 *
 * <p>前端拿到 {@code token} 后，后续请求带上 {@code Authorization: Bearer <token>}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT，前端需自行保存（建议内存或 localStorage，并注意 XSS 风险） */
    private String token;

    /** 登录用户信息，不含密码 */
    private UserVO user;
}
