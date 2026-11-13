# 第 07 周作业：接入 Nacos 注册中心与服务发现

## 1. 概述

第 06 周的两个业务服务（`resident-service`、`repair-service`）已接入 Nacos；本周按作业要求补充了第三个业务服务 `notice-service`，形成 3 个业务服务。全部服务按各自 `spring.application.name` 注册到默认命名空间、分组 `PERSONAL_GROUP`；跨服务流程由原先的固定地址改为 `DiscoveryClient` 服务发现调用。

| 服务名 | 容器端口 | 数据库 | 说明 |
| --- | --- | --- | --- |
| `resident-service` | 8081 | `resident_db` | 业主档案 |
| `repair-service` | 8082 | `repair_db` | 报修工单，调用业主服务 |
| `notice-service` | 8083 | `notice_db` | 社区公告 |

## 2. Nacos 部署与初始化

`compose.yaml` 中新增 `nacos/nacos-server:v3.2.4`，采用单机模式并开启鉴权：

- 控制台端口 8080 映射到宿主机 **18081**；服务端 8848 映射到 **18848**；SDK gRPC 9848 映射到 **19848**。
  - 说明：本机 18080/8848/9848 已被另一个本地工程占用，因此按作业要求改用空闲端口；容器内各服务仍通过 `nacos:8848` 访问。
- 数据持久化：命名数据卷 `nacos-data` 挂载到 `/home/nacos/data`，管理员密码、命名空间与服务数据在容器重建后保留。
- 鉴权参数通过 `.env` 注入：`NACOS_AUTH_TOKEN`（Base64，原文 ≥ 32 字节）、`NACOS_AUTH_IDENTITY_KEY/VALUE`、`NACOS_AUTH_ENABLE/ADMIN_ENABLE/CONSOLE_ENABLE=true`。
- 首次初始化管理员密码（仅首次执行）：

```bash
curl -X POST "http://localhost:18848/nacos/v3/auth/user/admin" -d "password=$NACOS_PASSWORD"
```

返回 `{"code":0,"message":"success",...}` 后，`nacos` 账号即可用 `.env` 中的密码登录控制台与 SDK。

### 注册结果

启动 3 个服务后，Nacos v3 管理接口返回 3 个服务、每个服务 1 个健康实例：

```
GET /nacos/v3/admin/ns/service/list?namespaceId=public&groupName=PERSONAL_GROUP
totalCount=3
- resident-service: groupName=PERSONAL_GROUP, ipCount=1, healthyInstanceCount=1
- notice-service:   groupName=PERSONAL_GROUP, ipCount=1, healthyInstanceCount=1
- repair-service:   groupName=PERSONAL_GROUP, ipCount=1, healthyInstanceCount=1
```

服务端注册日志（实例地址为本次运行观测值）：

```
nacos registry, PERSONAL_GROUP resident-service 172.20.0.8:8081 register finished
nacos registry, PERSONAL_GROUP repair-service 172.20.0.5:8082 register finished
nacos registry, PERSONAL_GROUP notice-service 172.20.0.2:8083 register finished
```

截图：`screenshots/01-nacos-service-list.png`（控制台服务列表，分组 `PERSONAL_GROUP`）。

## 3. 配置隔离

- 每个服务独立配置 `spring.application.name`，Compose 中三个容器端口互不冲突；`.env` 控制各数据库密码与 Nacos 凭据。
- Nacos 地址、账号、分组全部通过环境变量：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:}
        group: ${NACOS_GROUP:PERSONAL_GROUP}
```

Compose 内 `NACOS_SERVER_ADDR=nacos:8848`；宿主机调试时改为 `localhost:18848`。命名空间使用默认 `public`，未与课堂示例混用；分组统一为 `PERSONAL_GROUP`。

## 4. 服务发现调用（报修 -> 业主）

### 调用方向与实现

`repair-service` 的 `RepairTicketService.create()` 在校验业主时调用 `ResidentClient.ensureExists(residentId)`：

1. 每次调用都执行 `DiscoveryClient.getInstances("resident-service")`，不使用启动时缓存的地址。
2. 取实例列表中的第一个实例，用 `instance.getUri()` 拼接 `/api/residents/{id}`，通过 `RestTemplate` 发起 HTTP 请求。
3. 下游 404 映射为原有 404（业主不存在）；实例列表为空或连接失败/超时返回 503。
4. `RestTemplate` 配置 2s 连接超时、5s 读取超时，避免阻塞报修流程。
5. 已删除固定地址配置：`application.yaml` 中的 `services.resident-url` 与 Compose 中的 `RESIDENT_SERVICE_URL` 均已移除；仅保留注册中心地址与数据库配置。

运行日志示例（证明使用 Nacos 发现的实例地址，而非固定地址）：

```
服务发现 resident-service 实例 172.20.0.8#8081##PERSONAL_GROUP@@resident-service -> http://172.20.0.8:8081
```

### 请求示例与结果

```bash
# 前置：residentId=2 已存在
curl -X POST -H "Content-Type: application/json" -d '{
  "residentId":2,"title":"墙面渗水","description":"卫生间墙面渗水","location":"1号楼301"
}' http://localhost:8092/api/repair-tickets
```

| 场景 | 预期 | 实际 |
| --- | --- | --- |
| 全部依赖正常 | 201 + 工单记录 | 201，工单写入 `repair_db` |
| 业主不存在（residentId=999） | 404 | 404（未被改写成 503） |
| 业主服务停止 | 503 | 503，Nacos 中 `resident-service` 实例数为 0 |
| 业主服务恢复后重试 | 201 | 201，重新注册并恢复调用 |

截图：`screenshots/02-discovery-call-log.png`、`screenshots/04-dependent-down-503.png`、`screenshots/05-recovery-201.png`。

## 5. 上下线与健康验证

### 验证步骤（实际执行）

1. 三个服务均健康：`resident-service`、`repair-service`、`notice-service` 的 `/actuator/health` 均返回 `{"status":"UP"}`。
2. 执行 `docker compose stop resident-service`，等待 Nacos 摘除实例后再次创建报修：HTTP **503**；Nacos 服务列表显示 `resident-service` 的 `ipCount=0`，调用方没有绕过校验写入工单。
3. 执行 `docker compose start resident-service`，健康检查恢复 `UP`，实例重新注册；再次创建报修返回 **201**。

### SDK 连接、注册状态与 Actuator 健康的区别

- **SDK 连接**：Nacos 客户端的 gRPC 长连接是否可用，决定能否注册/心跳/查询实例；连接断开时服务发现调用会失败。
- **注册状态**：控制台中实例是否存在、是否 healthy，由 SDK 心跳维持；进程停止后实例会被摘除。
- **Actuator 健康**：应用自身（含数据库连接）的健康状态，`DOWN` 不会自动让 Nacos 摘除实例；注册正常也不等于业务健康。

截图：`screenshots/03-health-up.png`。

## 6. 冒烟脚本

`scripts/smoke-test.sh` 在根目录执行，检查项：

1. Nacos 控制台 `http://localhost:18081/` 可访问（HTTP 302，跟随重定向到登录页），不使用旧版 `8848/nacos` 控制台地址。
2. 三个服务的 `/actuator/health` 必须为 `UP`。
3. 业务链路：新增住户 -> 报修服务经服务发现创建工单（201）；业主不存在保留 404。
4. 公告服务新增（201）与列表查询（200）。

任一检查失败即 `exit 1`。本次执行结果：

```
[PASS] Nacos 控制台 http://localhost:18081 -> HTTP 302
[PASS] resident-service health -> {"groups":["liveness","readiness"],"status":"UP"}
[PASS] repair-service health -> ...
[PASS] notice-service health -> ...
[PASS] 新增住户 id=2
[PASS] 合法住户创建报修 -> HTTP 201
[PASS] 业主不存在 -> HTTP 404
[PASS] 新增公告 -> HTTP 201
[PASS] 公告列表查询 -> HTTP 200
[PASS] 冒烟测试全部通过
```

截图：`screenshots/06-smoke-test.png`。

## 7. 单元测试（不连接真实 Nacos/MySQL）

`services/repair-service` 增加 `ResidentClientTest`，Mock `DiscoveryClient` 与 `RestTemplate`，覆盖：

- 实例列表为空：抛出 `ServiceUnavailableException`，且不发起下游调用（503 语义）。
- 正常实例：使用实例 URI `http://127.0.0.1:8081/api/residents/7` 发起请求。
- 下游 404：映射为 `ResourceNotFoundException`。
- 连接失败：映射为 `ServiceUnavailableException`。

执行命令与结果：

```bash
cd services/repair-service
docker run --rm -v "${PWD}:/workspace" -w /workspace \
  maven:3.9.16-eclipse-temurin-25 mvn -B test
```

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 8. 截图清单

| 文件 | 内容 |
| --- | --- |
| `screenshots/01-nacos-service-list.png` | 控制台服务列表：3 个服务、分组 `PERSONAL_GROUP`、实例 IP/端口 |
| `screenshots/02-discovery-call-log.png` | 报修服务日志中的 `服务发现 resident-service ... -> http://...` |
| `screenshots/03-health-up.png` | 三个服务 `/actuator/health` 返回 UP |
| `screenshots/04-dependent-down-503.png` | 停止业主服务后创建报修返回 503，Nacos 实例数为 0 |
| `screenshots/05-recovery-201.png` | 恢复业主服务后重新注册并创建报修 201 |
| `screenshots/06-smoke-test.png` | `bash scripts/smoke-test.sh` 全部 PASS |
