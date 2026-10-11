package com.myshop.common;

/**
 * 业务异常：用于「用户名已存在」「用户名或密码错误」这类可预期的失败。
 *
 * <p>由 {@link GlobalExceptionHandler} 统一转成 {@link Result} 输出，
 * 避免把业务失败混进 500 服务器错误里。
 */
public class BusinessException extends RuntimeException {

    /** 业务状态码，如 400 参数问题、401 未登录、403 越权 */
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
