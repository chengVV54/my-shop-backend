package com.myshop.controller;

import com.myshop.common.Result;
import com.myshop.model.LoginRequest;
import com.myshop.model.LoginResponse;
import com.myshop.model.RegisterRequest;
import com.myshop.model.UserVO;
import com.myshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：注册、登录、获取当前登录用户。
 *
 * <p>{@code /register} 与 {@code /login} 匿名可访问，{@code /me} 需要携带
 * {@code Authorization: Bearer <token>}。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册。
     *
     * @param request 用户名（3~20 位）与密码（至少 6 位）
     * @return 新用户信息，不含密码
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 登录。
     *
     * @param request 用户名与密码
     * @return token 与用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 当前登录用户信息，需要登录。
     *
     * @return 当前用户信息，不含密码
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.getCurrentUser());
    }
}
