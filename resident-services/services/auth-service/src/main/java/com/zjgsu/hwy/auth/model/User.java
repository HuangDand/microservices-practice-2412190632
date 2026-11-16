package com.zjgsu.hwy.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 用户账号：密码只存 BCrypt 哈希，绝不存明文。
 * roles 以逗号分隔存储，例如 "ROLE_ADMIN" 或 "ROLE_RESIDENT,ROLE_ADMIN"。
 */
@Entity
@Table(name = "auth_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    /** BCrypt 哈希值 */
    @Column(nullable = false, length = 128)
    private String passwordHash;

    /** 逗号分隔的角色列表 */
    @Column(nullable = false, length = 256)
    private String roles;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // JPA 默认构造器
    public User() {}

    public User(String username, String passwordHash, String roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoles() { return roles; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }

    public java.util.List<String> roleList() {
        if (roles == null || roles.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(roles.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
