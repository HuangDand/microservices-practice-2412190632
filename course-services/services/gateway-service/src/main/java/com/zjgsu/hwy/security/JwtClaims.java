package com.zjgsu.hwy.security;

public record JwtClaims(String accountId, String username, String role, String studentId) {
}
