package com.myshop.config;

/**
 * 当前登录用户的线程上下文。
 *
 * <p>{@link JwtAuthenticationFilter} 在请求进入时写入，请求结束时清理，
 * 因此业务代码（Service）可以随时通过 {@link #getCurrentUserId()} 拿到操作者 ID。
 *
 * <p>鉴权之外，这个类也是「越权检查」的基础：凡是修改用户自有数据的操作，
 * 都要先取出当前用户 ID 与数据行的 {@code ownerId} / {@code sellerId} / {@code toUserId}
 * 比对，不相等直接拒绝，禁止信任前端传入的用户 ID。
 *
 * <p>注意：使用 {@link ThreadLocal} 时必须在 finally 中调用 {@link #clear()}，
 * 否则线程池复用会造成用户身份串号，属于严重安全问题。
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前登录用户，由 JWT 过滤器调用。
     */
    public static void set(Long userId, String username) {
        CURRENT_USER_ID.set(userId);
        CURRENT_USERNAME.set(username);
    }

    /**
     * 当前登录用户 ID。
     *
     * @return 已登录返回用户 ID，匿名访问返回 null
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 当前登录用户名。
     *
     * @return 已登录返回用户名，匿名访问返回 null
     */
    public static String getCurrentUsername() {
        return CURRENT_USERNAME.get();
    }

    /**
     * 清理上下文，必须在请求结束时调用。
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USERNAME.remove();
    }
}
