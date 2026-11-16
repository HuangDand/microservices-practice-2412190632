# 安全与鉴权设计

Week-09 引入 API Gateway + JWT 认证后，所有对业务服务的外部请求必须经过 Gateway。

## 认证流程

```
客户端                Gateway(8080)          Auth-Service          MySQL(auth_db)
  │
  ├─ POST /api/auth/login ──────────────────────────────────────────────────┐
  │   (no Token)                      (permitAll 路由，不过滤)           │
  │                                                                    │
  │                                     ├─ BCrypt 比对密码             │
  │                                     └─ 签 JWT (HS256)              │
  │                                      ↓                              │
  │   ◄────── { token, roles, ... } ────────────────────────────────────┘
  │
  ├─ GET /api/repair-tickets ────────────┐
  │   Authorization: Bearer <token>       │ Gateway JwtAuthFilter
  │                                        ├─ 验签 (HS256)
  │                                        ├─ 注入 X-Username / X-Roles / X-Gateway-Sign
  │                                        └─ 路由 lb://repair-service
  │
  │   (未登录 → 401; 角色不匹配 → 403)    │
```

## 网关路由与鉴权规则

Gateway 路由按 **更具体优先** 顺序匹配（`Method` 匹配先于纯 `Path`），metadata `requiredRole` 声明所需角色：

| 路由 | Path | Method | requiredRole | 说明 |
| --- | --- | --- | --- | --- |
| auth-route | `/api/auth/**` | 全部 | permitAll | 登录放行 |
| repair-admin-write | `/api/repair-tickets/**` | POST/PATCH/DELETE | ROLE_ADMIN | 工单写操作需管理员 |
| repair-route | `/api/repair-tickets/**` | GET/HEAD | — | 读操作登录即可 |
| resident-admin-write | `/api/residents/**` | POST/PATCH/DELETE | ROLE_ADMIN | 业主写操作需管理员 |
| resident-route | `/api/residents/**` | GET/HEAD | — | 读操作登录即可 |
| notice-admin-write | `/api/notices/**` | POST/PATCH/DELETE | ROLE_ADMIN | 公告写操作需管理员 |
| notice-route | `/api/notices/**` | GET/HEAD | — | 读操作登录即可 |

> 默认 deny 策略：未配置 `auth: permitAll` 或 `requiredRole` 的路由都要求携带有效 Token，但不限制角色（登录即可）。

## 测试账号

| 用户名 | 密码 | 角色 | 能力 |
| --- | --- | --- | --- |
| `admin` | `admin123` | `ROLE_ADMIN` | 读写所有资源 |
| `resident1` | `resident123` | `ROLE_RESIDENT` | 只读，无法 POST/PATCH/DELETE |

密码用 BCrypt 哈希存储，源码和数据库中无明文。

## 防绕过机制

Gateway 会在转发时注入三个可信头，下游服务可据此确认请求确实经过网关，而非绕过网关直连：

- `X-Username`：网关注入的认证用户名（客户端伪造会被 Gateway 先删除再设值）
- `X-Roles`：网关注入的角色列表（逗号分隔）
- `X-Gateway-Sign`：`HMAC_SHA256(JWT_SECRET, username)`，下游用同一个密钥验证即可

配套措施：

1. 业务服务容器**不映射宿主机端口**，外部只能访问 Gateway。
2. Gateway 容器端口 8080 是唯一开放入口。
3. Auth-Service 同样不映射宿主机端口——对外只能经 Gateway 登录。

## 密钥管理

- JWT 签名密钥通过环境变量 `JWT_SECRET` 注入 Gateway 和 Auth-Service，源码中不出现真实密钥。
- `.env.example` 只给占位值，真实值在本机 `.env`（`.gitignore` 排除）。
- Gateway 验签密钥和 Auth-Service 签发密钥必须完全一致。
- 密钥兼容 Base64 编码和原样字节两种形式。

## 运行验证

```bash
# 统一入口健康检查
curl http://localhost:8080/actuator/health

# 登录拿 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# 携带 Token 访问受保护资源
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/repair-tickets

# 端到端鉴权验证脚本（10 个检查点）
bash scripts/auth-test.sh
```
