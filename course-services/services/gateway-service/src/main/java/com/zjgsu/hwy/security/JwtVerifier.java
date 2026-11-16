package com.zjgsu.hwy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置，无法校验 Token");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public JwtClaims verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaims(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claims.get("studentId", String.class));
    }
}
