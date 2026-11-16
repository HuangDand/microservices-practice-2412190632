package com.zjgsu.hwy.filter;

import com.zjgsu.hwy.security.JwtClaims;
import com.zjgsu.hwy.security.JwtVerifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LOGIN_PATH = "/api/auth/login";

    private final JwtVerifier jwtVerifier;

    public JwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = String.valueOf(exchange.getRequest().getMethod());

        if (LOGIN_PATH.equals(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "unauthorized", "missing bearer token");
        }

        JwtClaims claims;
        try {
            claims = jwtVerifier.verify(authorization.substring(BEARER_PREFIX.length()));
        } catch (Exception e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "invalid_token", "token is invalid or expired");
        }

        if ("POST".equals(method) && "/api/courses".equals(path) && !"ADMIN".equals(claims.role())) {
            return reject(exchange, HttpStatus.FORBIDDEN, "forbidden", "only ADMIN can create courses");
        }
        if ("POST".equals(method) && "/api/enrollments".equals(path) && !"STUDENT".equals(claims.role())) {
            return reject(exchange, HttpStatus.FORBIDDEN, "forbidden", "only STUDENT can enroll");
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Username");
                    headers.remove("X-User-Role");
                    headers.set("X-User-Id", nullToEmpty(claims.accountId()));
                    headers.set("X-Username", nullToEmpty(claims.username()));
                    headers.set("X-User-Role", nullToEmpty(claims.role()));
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
