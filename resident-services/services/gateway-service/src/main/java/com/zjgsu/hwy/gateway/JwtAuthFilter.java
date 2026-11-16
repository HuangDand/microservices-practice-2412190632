package com.zjgsu.hwy.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 全局鉴权过滤器（默认 deny 策略）：
 * <ol>
 *   <li>route metadata auth: permitAll → 直接放行</li>
 *   <li>无 Authorization / Token 无效 → 401</li>
 *   <li>route metadata requiredRole 且用户没有 → 403</li>
 *   <li>成功 → 注入 X-Username / X-Roles / X-Gateway-Sign 转发</li>
 * </ol>
 * 注意：Gateway 是 WebFlux，返回 Mono<Void>；不要对 chain.filter 用 switchIfEmpty 兜底。
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /** 网关注入的签名头，下游可验证请求来自网关。 */
    public static final String HEADER_GATEWAY_SIGN = "X-Gateway-Sign";

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Value("${auth.jwt.prefix:Bearer }")
    private String tokenPrefix;

    @Value("${auth.jwt.header:Authorization}")
    private String authHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        // 1) permitAll 放行（登录、健康检查等）
        if (route != null && "permitAll".equals(route.getMetadata().get("auth"))) {
            return chain.filter(exchange);
        }

        // 2) 取 Token
        ServerHttpRequest request = exchange.getRequest();
        String headerVal = request.getHeaders().getFirst(authHeader);
        if (headerVal == null || !headerVal.startsWith(tokenPrefix)) {
            log.debug("[Auth] 无 Token: {} {}", request.getMethod(), request.getURI().getPath());
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized: missing token");
        }
        String token = headerVal.substring(tokenPrefix.length()).trim();

        // 3) 验签
        Claims claims;
        try {
            SecretKey key = decodeKey(jwtSecret);
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            claims = jws.getPayload();
        } catch (Exception e) {
            log.warn("[Auth] Token 验签失败: {}", e.getMessage());
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized: invalid token");
        }

        // 4) 角色校验
        if (route != null && route.getMetadata().get("requiredRole") != null) {
            String requiredRole = route.getMetadata().get("requiredRole").toString();
            @SuppressWarnings("unchecked")
            List<String> userRoles = claims.get("roles", List.class);
            if (userRoles == null || !userRoles.contains(requiredRole)) {
                log.info("[Auth] 越权拒绝: path={} roles={} required={}",
                        request.getURI().getPath(), userRoles, requiredRole);
                return writeError(exchange, HttpStatus.FORBIDDEN,
                        "Forbidden: insufficient role, need " + requiredRole);
            }
        }

        // 5) 注入可信身份头（先删客户端伪造的，再设网关值）
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.remove("X-Username");
                    h.remove("X-Roles");
                    h.remove(HEADER_GATEWAY_SIGN);
                })
                .header("X-Username", username)
                .header("X-Roles", roles == null ? "" : String.join(",", roles))
                .header(HEADER_GATEWAY_SIGN, computeGatewaySign(jwtSecret, username))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /** 网关签名：HMAC(secret, username)，下游验证即可确认请求经网关转发。 */
    private static String computeGatewaySign(String secret, String username) {
        try {
            SecretKey key = decodeKey(secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal(username.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("gateway sign failed", e);
        }
    }

    private static SecretKey decodeKey(String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
