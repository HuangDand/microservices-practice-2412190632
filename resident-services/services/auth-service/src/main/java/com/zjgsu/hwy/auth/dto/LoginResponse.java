package com.zjgsu.hwy.auth.dto;

import com.zjgsu.hwy.auth.service.AuthService;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        List<String> roles,
        Instant expiresAt
) {
    public static LoginResponse from(AuthService.LoginResult r) {
        return new LoginResponse(r.token(), r.tokenType(), r.username(), r.roles(), r.expiresAt());
    }
}
