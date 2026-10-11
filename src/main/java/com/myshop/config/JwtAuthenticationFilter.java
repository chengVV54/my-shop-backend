package com.myshop.config;

import com.myshop.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：从 {@code Authorization: Bearer <token>} 中解析用户身份。
 *
 * <p>行为约定：
 * <ul>
 *     <li>没有 token：直接放行，是否允许访问交给 SecurityConfig 的授权规则决定（匿名可访问的接口不受影响）</li>
 *     <li>token 有效：把 userId / username 写入 {@link UserContext} 与 SecurityContext</li>
 *     <li>token 无效或过期：按未登录处理，不再抛出异常，受保护接口最终由 SecurityConfig 返回 401</li>
 * </ul>
 *
 * <p>身份信息全部来自服务端签名校验过的 token，不信任任何请求参数中的用户 ID。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.parseToken(token);
                String username = jwtUtil.parseUsername(token);

                UserContext.set(userId, username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } finally {
            // 线程池复用线程，必须清理，避免身份串号
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 从请求头中取出 Bearer token。
     *
     * @return 无 token 或格式不是 Bearer 时返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
