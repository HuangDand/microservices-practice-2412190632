package com.zjgsu.hwy.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证入口。仅暴露登录接口，其他账号管理接口按作业要求可选补充。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final com.zjgsu.hwy.auth.service.AuthService authService;

    public AuthController(com.zjgsu.hwy.auth.service.AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<com.zjgsu.hwy.auth.dto.LoginResponse> login(
            @Valid @RequestBody com.zjgsu.hwy.auth.dto.LoginRequest req) {
        var result = authService.login(req.username(), req.password());
        return ResponseEntity.ok(com.zjgsu.hwy.auth.dto.LoginResponse.from(result));
    }

    /** 轻量健康检查，让 Gateway 的 permitAll 路由能看到存活。 */
    @PostMapping("/test-login")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("auth-service-ready");
    }
}
