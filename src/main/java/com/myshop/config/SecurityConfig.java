package com.myshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myshop.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置：前后端分离 + JWT + BCrypt。
 *
 * <p>访问规则：
 * <ul>
 *     <li>匿名可访问：{@code /api/health}、{@code /api/auth/register}、{@code /api/auth/login}、
 *         {@code GET /api/items/**}（物品浏览无需登录）</li>
 *     <li>需要登录：{@code /api/auth/me}、{@code /api/barters/**}、{@code /api/orders/**}，
 *         以及其余所有 {@code /api/**}（为后续发布物品等写操作预留）</li>
 * </ul>
 *
 * <p>注意：{@code /api/auth/**} 不能整体放行，否则 {@code /api/auth/me} 会变成匿名可访问，
 * 因此这里只精确放行注册与登录两个接口。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 前端开发服务器（Vite 默认端口） */
    private static final List<String> ALLOWED_ORIGINS =
            List.of("http://localhost:5173", "http://127.0.0.1:5173");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper;

    /**
     * 密码加密器：BCrypt（自带随机盐，同一明文每次哈希结果不同）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置，供 Security 的 CorsFilter 使用。
     *
     * <p>与 {@code CorsConfig}（MVC 层）的取值保持一致，两边都不会引入更宽松的来源。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 阻止 Spring Boot 把 {@link JwtAuthenticationFilter} 当成普通 Servlet Filter 再自动注册一次。
     *
     * <p>否则同一个过滤器实例会被注册两次（容器一次 + Security 链一次），
     * 执行顺序不再由 {@code addFilterBefore} 决定，容易排查困难。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离使用 Bearer Token，不依赖 Cookie，因此关闭 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // 纯 API 服务，关闭表单登录、HTTP Basic 与登出页
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 无状态：不创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot 的错误转发页，放行避免 404/500 被改写成 401
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // 物品浏览匿名开放；后续新增的 POST / PUT / DELETE 会自动落到下面的 /api/** 需要登录
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // 未登录 / 越权统一返回项目约定的 JSON 结构，而不是空响应体
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(this::handleUnauthenticated)
                        .accessDeniedHandler(this::handleAccessDenied))
                // JWT 过滤器放在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 未认证（无 token / token 失效）时返回 401。
     */
    private void handleUnauthenticated(HttpServletRequest request,
                                       HttpServletResponse response,
                                       AuthenticationException ex) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.error(401, "未登录或登录已过期"));
    }

    /**
     * 已登录但权限不足时返回 403。
     */
    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    AccessDeniedException ex) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, Result.error(403, "没有权限执行该操作"));
    }

    private void writeJson(HttpServletResponse response, int httpStatus, Result<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
