package com.myshop.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，基于 jjwt 0.12.x。
 *
 * <p>签名密钥与有效期全部来自配置（{@code jwt.secret} / {@code jwt.expiration}），
 * 代码里不写死密钥。application.yml 中的默认值可用环境变量 {@code JWT_SECRET} 覆盖。
 *
 * <p>Token 载荷约定：{@code sub} 存 userId，{@code username} 存用户名。
 */
@Component
public class JwtUtil {

    /** 用户名 claim 的 key */
    private static final String CLAIM_USERNAME = "username";

    /** HS256 要求密钥长度不少于 32 字节（256 位） */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    /** 有效期，毫秒 */
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 长度不足 " + MIN_SECRET_BYTES + " 字节，无法用于 HS256 签名");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 token。
     *
     * @param userId   用户 ID，写入 {@code sub}
     * @param username 用户名，写入 {@code username} claim
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token 并返回其中的用户 ID。
     *
     * @param token JWT 字符串
     * @return userId
     * @throws JwtException             签名非法、已过期、格式错误等
     * @throws IllegalArgumentException token 为空或 userId 非数字
     */
    public Long parseToken(String token) {
        Claims claims = parseClaims(token);
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("token 中的用户 ID 非法", ex);
        }
    }

    /**
     * 解析 token 并返回其中的用户名。
     *
     * @param token JWT 字符串
     * @return username，缺失时返回 null
     */
    public String parseUsername(String token) {
        return parseClaims(token).get(CLAIM_USERNAME, String.class);
    }

    /**
     * 校验 token 是否有效（签名正确且未过期）。不抛异常，便于过滤器直接判断。
     *
     * @param token JWT 字符串
     * @return 有效返回 true，否则 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 校验签名并取出载荷。
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
