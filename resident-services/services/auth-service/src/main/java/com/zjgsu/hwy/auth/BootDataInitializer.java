package com.zjgsu.hwy.auth;

import com.zjgsu.hwy.auth.model.User;
import com.zjgsu.hwy.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动时 upsert 预置账号：admin / resident1。
 * 重复启动幂等（已存在则跳过），密码用 BCryptPasswordEncoder 哈希，绝不存明文。
 */
@Component
public class BootDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootDataInitializer.class);

    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;

    @Value("${auth.bootstrap.admin-username:admin}")
    private String adminUsername;
    @Value("${auth.bootstrap.admin-password:admin123}")
    private String adminPassword;
    @Value("${auth.bootstrap.resident-username:resident1}")
    private String residentUsername;
    @Value("${auth.bootstrap.resident-password:resident123}")
    private String residentPassword;

    public BootDataInitializer(UserRepository users, BCryptPasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        ensure(adminUsername, adminPassword, "ROLE_ADMIN");
        ensure(residentUsername, residentPassword, "ROLE_RESIDENT");
    }

    private void ensure(String username, String rawPassword, String roles) {
        if (users.existsByUsername(username)) {
            log.info("[Auth] 账号已存在，跳过: {}", username);
            return;
        }
        User u = new User(username, encoder.encode(rawPassword), roles);
        users.save(u);
        log.info("[Auth] 预置账号: username={} roles={}", username, roles);
    }
}
