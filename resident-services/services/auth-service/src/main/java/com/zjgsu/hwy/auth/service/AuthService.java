package com.zjgsu.hwy.auth.service;

import com.zjgsu.hwy.auth.model.User;
import com.zjgsu.hwy.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 认证核心服务：登录校验 + JWT 签发。
 *
 * <p>密码校验：Spring Security Crypto 的 BCryptPasswordEncoder（不引入整套 Spring Security）。
 * JWT 签发：HS256 + claims 包含 sub / roles / iss / exp。密钥解码兼容 Base64 和原文。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecretKey signKey;
    private final String issuer;
    private final long expirationMinutes;

    public AuthService(UserRepository users, BCryptPasswordEncoder passwordEncoder,
                       @Value("${auth.jwt.secret}") String jwtSecret,
                       @Value("${auth.jwt.issuer}") String issuer,
                       @Value("${auth.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.signKey = decodeKey(jwtSecret);
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * 登录：返回 token，失败抛 IllegalArgumentException（由全局 handler 映射 401）。
     */
    public LoginResult login(String username, String rawPassword) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("账号已禁用");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.info("[Auth] 密码校验失败: {}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        List<String> roles = user.roleList();

        String token = Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuer(issuer)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(exp))
                .signWith(signKey)
                .compact();

        return new LoginResult(token, "Bearer", username, roles, exp);
    }

    /** 和 Gateway 共用的密钥解码逻辑。 */
    private static SecretKey decodeKey(String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public record LoginResult(String token, String tokenType, String username,
                              List<String> roles, Instant expiresAt) {}
}
