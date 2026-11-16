# Week-09 — API Gateway + JWT 认证

## 总体架构

```
  客户端
    │
    ├─ Gateway :8080 (统一入口 / 健康检查)
    │   ├─ lb://auth-service     ← /api/auth/**  (permitAll)
    │   ├─ lb://repair-service   ← /api/repair-tickets/**
    │   ├─ lb://resident-service ← /api/residents/**
    │   └─ lb://notice-service   ← /api/notices/**
    │
    └─ Auth-Service :8084 (仅内网)
        └─ MySQL auth_db (auth_users)
```

业务服务端口 8081 / 8082 / 8083 **不映射宿主机端口**，外部只能经 Gateway 访问。

---

## 1. API Gateway

### 实现思路

新建 `services/gateway-service`，基于 **Spring Cloud Gateway 2025.1.0**（WebFlux 栈）+ Nacos Discovery。

- 统一对外端口 8080。
- 显式声明 7 条路由（含 permitAll 与 requiredRole 变体）。
- 健康检查入口使用 Gateway 自带的 `/actuator/health`，不额外占用业务路由。
- 注意：Spring Cloud Gateway 5.0.0 起配置前缀为 `spring.cloud.gateway.server.webflux.routes`，旧的 `spring.cloud.gateway.routes` 会被静默忽略并导致所有请求 404。
- URI 全部用 `lb://服务名`，由 LoadBalancer 在 Nacos 注册实例间分发。
- 保留 `/actuator/health` 内置健康检查。

### 路由表（节选）

| 路由 | Path | Method | requiredRole | 说明 |
| --- | --- | --- | --- | --- |
| auth-route | `/api/auth/**` | 全部 | permitAll | 登录放行 |
| repair-admin-write | `/api/repair-tickets/**` | POST/PATCH/DELETE | ROLE_ADMIN | 工单写 |
| repair-route | `/api/repair-tickets/**` | GET/HEAD | — | 工单读 |
| resident-admin-write | `/api/residents/**` | POST/PATCH/DELETE | ROLE_ADMIN | 业主写 |
| resident-route | `/api/residents/**` | GET/HEAD | — | 业主读 |
| notice-admin-write | `/api/notices/**` | POST/PATCH/DELETE | ROLE_ADMIN | 公告写 |
| notice-route | `/api/notices/**` | GET/HEAD | — | 公告读 |

### 运行命令

```bash
# 启动（Gateway 依赖 Nacos 和 auth-service 健康）
docker compose up -d --build gateway-service auth-service

# 健康检查
curl http://localhost:8080/actuator/health
# 预期：{"status":"UP"}
```

### 验证方法

```bash
# 未登录访问 → 401（证明 Gateway 拦截生效）
curl -i http://localhost:8080/api/repair-tickets | head -5
# 预期 HTTP/1.1 401

# 登录（网关代理到 auth-service）
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 预期 200 + token
```

![screenshot](screenshots/01-gateway-routes.png)

---

## 2. 认证服务

### 实现思路

新建 `services/auth-service`，职责单一：账号管理 + 登录 + JWT 签发。

- 账号表 `auth_users`：`username`、`password_hash`、`roles`（逗号分隔字符串）。
- 密码存储：**BCrypt**（`spring-security-crypto`，不引入整套 Spring Security）。
- JWT：HS256 + JJWT（jjwt-api/impl/jackson 0.12.6），claims 含 `sub` / `roles` / `iss` / `exp`，有效期 120 分钟。
- 密钥来源：环境变量 `JWT_SECRET`（Base64，Gateway 与 Auth 共享），源码无真实密钥。
- 启动时 `CommandLineRunner` **幂等 upsert** 两个预置账号（admin / resident1）。

### 预置账号

| 用户名 | 密码 | 角色 | 用途 |
| --- | --- | --- | --- |
| `admin` | `admin123` | `ROLE_ADMIN` | 管理员，可读写所有资源 |
| `resident1` | `resident123` | `ROLE_RESIDENT` | 普通业主，只读 |

密码在 auth_db 中以 `$2a$10$...` 开头的 BCrypt 哈希存储。

### 运行命令

```bash
# 单独启动 auth-service
docker compose up -d --build auth-service

# 查看容器日志（确认预置账号）
docker logs resident-services-auth-service-1 | grep "预置账号"
# 预期：
#   [Auth] 预置账号: username=admin roles=ROLE_ADMIN
#   [Auth] 预置账号: username=resident1 roles=ROLE_RESIDENT
```

### 验证方法

```bash
# 直连 auth-service 登录（绕过 Gateway 的测试入口）
docker exec resident-services-gateway-service-1 curl -s \
  -X POST http://auth-service:8084/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 预期：{"token":"eyJhbG...","tokenType":"Bearer","username":"admin","roles":["ROLE_ADMIN"],...}

# 错误密码
curl -i http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"wrong"}' | head -5
# 预期 HTTP/1.1 401
```

![screenshot](screenshots/02-auth-login.png)

---

## 3. 网关鉴权

### 实现思路

Gateway 内 `JwtAuthFilter`（`GlobalFilter`，`@Order = -100`）完成三层检查，**默认 deny**：

1. 路由 metadata `auth: permitAll` → 直接放行（登录、健康检查）。
2. 无 `Authorization: Bearer <token>` 或 Token 验签失败/过期 → 401。
3. 路由 metadata `requiredRole` 且用户无该角色 → 403。

验签成功后，Gateway 在转发请求上**先删后注**三个可信头：

| Header | 来源 | 作用 |
| --- | --- | --- |
| `X-Username` | JWT `sub` | 当前登录用户 |
| `X-Roles` | JWT `roles` | 逗号分隔角色列表 |
| `X-Gateway-Sign` | `HMAC_SHA256(JWT_SECRET, username)` | 下游可验证请求来自网关 |

**防客户端伪造**：Filter 先用 `headers.remove(...)` 删掉所有同名头，再设网关注入的值，确保下游看到的一定是网关写的值。

**防绕过网关直连**：业务服务容器不映射宿主机端口，外部只能访问 Gateway 8080。

### 运行命令

```bash
# 确保全部服务在跑
docker compose ps

# 端到端鉴权验证（覆盖 4 个场景）
bash scripts/auth-test.sh
```

### 验证方法

```bash
# 1) 未登录访问受保护接口 → 401
curl -i http://localhost:8080/api/repair-tickets | head -5

# 2) 登录拿 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# 3) 携带 Token 访问 → 200
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/repair-tickets | head -5

# 4) 业主 Token 越权写工单 → 403
RESIDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"resident1","password":"resident123"}' | jq -r .token)

curl -i -X POST \
  -H "Authorization: Bearer $RESIDENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"residentId":1,"title":"越权测试","description":"x","location":"y"}' \
  http://localhost:8080/api/repair-tickets | head -5
# 预期 HTTP/1.1 403

# 5) 伪造客户端身份头被清掉（绕过 Gateway 直连不可行）
curl -i -H "X-Username: admin" http://localhost:8080/api/repair-tickets | head -5
# 预期 HTTP/1.1 401（Filter 在 Token 校验阶段拦截）
```

![screenshot](screenshots/03-auth-flow.png)

---

## 4. 测试脚本

### 实现思路

新建 `scripts/auth-test.sh`，覆盖作业要求的 4 个核心场景，在 Gateway 统一入口（默认 8080）上执行：

1. **未登录访问受保护接口 → 401**
2. **登录 admin / resident1 拿 Token → 200 + token 字段**
3. **携带 Token 访问受保护接口 → 200**
4. **低权限 resident Token 越权写工单 → 403**
5. **管理员 Token 写工单 → 201**

脚本用 Bash 原生 `assert_code` 函数按 HTTP 状态码判定，PASS/FAIL 计数并输出汇总。失败即退出非零。

### 运行命令

```bash
# 设置 Nacos 密码（已在 .env 中的话可直接 source）
export NACOS_PASSWORD="your-nacos-password"

# 执行
bash scripts/auth-test.sh

# 输出示例：
# --- 0/4 网关就绪检查 ---
# [PASS] Gateway 健康检查 UP
# --- 1/4 未登录访问应被拒绝 ---
# [PASS] 未登录访问 GET /api/repair-tickets → 401 (HTTP 401)
# --- 2/4 登录获取 Token ---
# [PASS] 管理员登录 HTTP 200 (HTTP 200)
# [PASS] 获取管理员 JWT (长度 175)
# [PASS] 业主登录 HTTP 200 (HTTP 200)
# [PASS] 获取业主 JWT (长度 175)
# --- 3/4 携带 Token 应成功访问 ---
# [PASS] 业主 Token 访问 GET /api/repair-tickets → 200 (HTTP 200)
# [PASS] 管理员 Token 访问 GET /api/repair-tickets → 200 (HTTP 200)
# --- 4/4 低权限越权应被拒绝 ---
# [PASS] 业主 Token 越权 POST /api/repair-tickets → 403 (HTTP 403)
# [PASS] 管理员 Token POST /api/repair-tickets → 201 (HTTP 201)
# =============================================
#   PASS=10  FAIL=0
# =============================================
```

### 验证方法

```bash
# 手工对每个场景确认（见第 3 节）
# 脚本可重复运行，幂等

bash scripts/auth-test.sh; echo "exit=$?"
# 预期 exit=0
```

![screenshot](screenshots/04-auth-test-script.png)

---

## 项目结构

```
resident-services/
├── services/
│   ├── gateway-service/     ← 新增（端口 8080，WebFlux）
│   │   ├── src/main/java/com/zjgsu/hwy/gateway/
│   │   │   ├── GatewayServiceApplication.java
│   │   │   └── JwtAuthFilter.java
│   │   ├── src/main/resources/application.yaml
│   │   ├── dockerfile
│   │   └── pom.xml
│   ├── auth-service/        ← 新增（端口 8084）
│   │   ├── src/main/java/com/zjgsu/hwy/auth/
│   │   │   ├── AuthServiceApplication.java
│   │   │   ├── AuthConfig.java          ← BCryptPasswordEncoder Bean
│   │   │   ├── AuthExceptionHandler.java ← 401 映射
│   │   │   ├── BootDataInitializer.java ← 预置账号
│   │   │   ├── controller/AuthController.java
│   │   │   ├── service/AuthService.java  ← BCrypt + JWT 签发
│   │   │   ├── model/User.java
│   │   │   ├── repository/UserRepository.java
│   │   │   └── dto/
│   │   ├── src/main/resources/application.yaml
│   │   ├── dockerfile
│   │   └── pom.xml
│   └── (resident / repair / notice — 无改动)
├── scripts/
│   └── auth-test.sh          ← 新增
├── docs/
│   ├── security.md           ← 新增
│   └── homework/week-09/
│       ├── index.md          ← 本文件
│       └── screenshots/
├── compose.yaml              ← 更新（+gateway +auth +auth-db）
└── .env.example              ← 更新（+JWT_SECRET +AUTH_DB_PASSWORD）
```
