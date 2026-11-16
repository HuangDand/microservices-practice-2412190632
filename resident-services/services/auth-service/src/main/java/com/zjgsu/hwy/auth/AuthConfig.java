package com.zjgsu.hwy.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AuthConfig {

    /**
     * 仅引入 spring-security-crypto，因此需要手动声明 BCryptPasswordEncoder。
     * 默认 strength=10，适合本地开发；生产可调到 12+。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
