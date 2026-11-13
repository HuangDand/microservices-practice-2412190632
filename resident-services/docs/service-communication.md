# 跨服务业务流程调用链路

> 本文档记录个人微服务项目的跨服务同步调用设计，配合 OpenFeign + Nacos + Spring Cloud LoadBalancer 完成 week-08 作业。

## 1. 参与角色

| 角色 | 服务名 | 说明 |
| --- | --- | --- |
| 调用方 | `repair-service` | 创建报修工单前，必须校验业主存在 |
| 被调用方 | `resident-service` | 提供业主档案查询接口 |

## 2. 调用链路图

```
                    ┌──────────────────────┐
                    │     Nacos 注册中心    │
                    │  PERSONAL_GROUP       │
                    │  resident-service: 2  │
                    └──────────────────────┘
                                 │ 注册/心跳
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
            ┌──────────────┐           ┌──────────────┐
            │ resident-#1  │           │ resident-#2  │
            │ 172.20.0.3   │           │ 172.20.0.7   │
            │  :8081       │           │  :8081       │
            └──────────▲───┘           └──────────▲───┘
                       │                          │
                       │   Spring Cloud LoadBalancer (轮询)
                       │                          │
              ┌────────┴──────────────────────────┴────────┐
              │                                             │
              │   resident-service 服务名                   │
              │   (OpenFeign @FeignClient 解析)             │
              └────────▲───────────────────────────────────┘
                       │ HTTP GET /api/residents/{id}
                       │ 超时: 2s connect / 5s read
              ┌────────┴────────┐
              │ repair-service  │  RepaiTicketService.create()
              │                 │  → ResidentClient.ensureExists()
              └─────────────────┘
```

## 3. 关键代码路径

```
RepairTicketService.create(CreateRepairTicketRequest)
  └─ ResidentClient.ensureExists(Long residentId)
       └─ ResidentFeignClient.exists(Long id)     ← OpenFeign 代理
            │  @FeignClient(name="resident-service")
            │  → Spring Cloud LoadBalancer 选实例
            │  → 发起 HTTP GET /api/residents/{id}
            │
            ├─ 200 OK      → 返回 ResidentExistsResponse
            ├─ 404 Not Found → FeignException.NotFound → 映射为 ResourceNotFoundException
            ├─ 5xx / timeout  → ResidentFeignFallback → ServiceUnavailableException
            └─ 无可用实例    → ResidentFeignFallback → ServiceUnavailableException
```

## 4. 边界与错误语义

| 场景 | 处理 |
| --- | --- |
| 业主不存在（404） | 保留原有 `ResourceNotFoundException`，不影响已有业务语义 |
| 下游实例为空 / 网络超时 / 5xx | Fallback 触发，抛 `ServiceUnavailableException` → HTTP 503 |
| LoadBalancer 默认不重试 | 避免写操作部分成功后重复调用 |
| 写操作部分成功 | 不处理——作业明确要求"不要求自动补偿或分布式事务" |

## 5. 负载均衡验证方法

1. 启动 `resident-service` 2 个实例：`docker compose up -d --scale resident-service=2 resident-service`
2. 两个实例都注册到 Nacos（`ipCount=2`）
3. 连续发起 10+ 次 `POST /api/repair-tickets`
4. 从以下两个来源验证 LB 分发：
   - **repair-service 日志**：Feign BASIC 级别日志会打印请求 URL，观察 host 变化
   - **resident-service 响应头** `X-Instance-Id`：每个实例返回自己的 hostname#PID
