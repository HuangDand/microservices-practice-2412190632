package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Account;
import com.zjgsu.hwy.security.JwtService;
import com.zjgsu.hwy.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Account> account = authService.authenticate(request.username(), request.password());
        return account
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(
                        new LoginResponse(jwtService.issue(a), a.getUsername(), a.getRole().name(), a.getStudentId())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_credentials", "message", "用户名或密码错误")));
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token, String username, String role, String studentId) {}
}
