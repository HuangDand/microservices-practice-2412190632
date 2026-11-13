# 第 08 周作业：OpenFeign 跨服务调用与负载均衡

## 1. 概述

在 week-07 的 Nacos 服务发现基础上，将 repair-service 与 resident-service 之间的调用从 week-07 的 `DiscoveryClient.getInstances()` + `RestTemplate` 替换为 **Spring Cloud OpenFeign + Spring Cloud LoadBalancer**。被调用方 resident-service 启动 2 个实例，由 LoadBalancer 自动分发，验证请求确实命中两个实例而非单一固定地址。

本次作业的跨服务业务流程与 week-07 一致：**创建报修工单前必须校验业主是否存在**。调用方 `repair-service`，被调用方 `resident-service`，接口 `GET /api/residents/{id}`。

调用链路图见：[docs/service-communication.md](../service-communication.md)。

## 2. OpenFeign 实现

### 依赖（repair-service/pom.xml）

复用 Spring Cloud 2025.1.0 + Spring Cloud Alibaba 2025.1.0.0 BOM，不显式覆盖 OpenFeign / LoadBalancer 版本：

```xml
<dependency><groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
<dependency><groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId></dependency>
```

### 启用 Feign（RepairServiceApplication）

```java
@SpringBootApplication
@EnableFeignClients
public class RepairServiceApplication { ... }
```

### Feign 接口 + Fallback

`ResidentFeignClient.java`：

```java
@FeignClient(
    name = "resident-service",
    path = "/api/residents"
)
public interface ResidentFeignClient {
    @GetMapping("/{id}")
    ResidentExistsResponse exists(@PathVariable("id") Long id);
}
```

> 说明：`@FeignClient(fallback = ...)` 只有在引入断路器实现（如 Resilience4j）并开启
> `spring.cloud.openfeign.circuitbreaker.enabled` 时才生效，否则会被静默忽略。
> 本项目未引入断路器，因此调用失败的映射放在 `ResidentClient` 中显式处理，
> 既能保证真实生效，也便于单元测试。

### 调用失败处理（ResidentClient）

Feign 接口只抛 `FeignException` / 运行时异常，业务层统一映射为业务异常：

| 调用结果 | 业务异常 | HTTP 语义 |
| --- | --- | --- |
| 下游 404（`FeignException.NotFound`） | `ResourceNotFoundException` | 业主不存在 |
| 下游 5xx（`FeignException`） | `ServiceUnavailableException` | 依赖返回异常 |
| LoadBalancer 无可用实例 / 连接被拒 / 读超时（`RuntimeException`） | `ServiceUnavailableException` | 依赖不可用 |

`ServiceUnavailableException` 由 `ApiExceptionHandler` 映射为 HTTP 503，因此
“业主服务不可用”不会被当成业务成功继续写入工单。

### 超时配置（application.yaml）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 2000   # 2s 连接超时
            read-timeout: 5000      # 5s 读取超时
            logger-level: BASIC     # 打印 URL + 状态码，便于 LB 验证
      loadbalancer:
        default:
          retry:
            enabled: false          # 不重试，避免写操作部分成功后重复调用
```

### 调用结果

执行 `docker compose build --no-cache resident-service repair-service && docker compose up -d` 启动新代码后：

```
repair-service 日志（Feign BASIC）：
[ResidentClient] Feign 调用 resident-service OK, residentId=7

单元测试（6/6）：
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

截图：`screenshots/01-feign-client-ok.png`、`screenshots/02-fallback-log.png`。

## 3. 负载均衡验证

### 步骤

```bash
# 1) 先停掉旧的 resident-service 单实例，重建为双实例
docker compose stop resident-service
docker compose up -d --scale resident-service=2 resident-service

# 2) 等两个实例都注册到 Nacos
for i in $(seq 1 20); do
  cnt=$(curl -sf "http://localhost:18848/nacos/v3/admin/ns/instance/list?accessToken=$TOKEN&..." | grep -o instanceId | wc -l)
  [ $cnt -ge 2 ] && break
  sleep 3
done

# 3) 连续 12 次跨服务创建报修
for i in $(seq 1 12); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "http://localhost:8092/api/repair-tickets" \
      -H "Content-Type: application/json" \
      -d "{\"residentId\":$RID,\"title\":\"LB-test-$i\",...}"
done
```

### 验证方法（两个可核验的证据点）

1. **repair-service Feign 日志**：BASIC 级别会打印每次请求的 URL 包括实际 host。观察是否在 `172.20.0.x:8081` 与 `172.20.0.y:8081` 之间轮换。
2. **resident-service 响应头 `X-Instance-Id`**：每个实例返回自己的 `hostname#PID`（由 ResidentController 在 GET / POST /api/residents 时写入），curl 用 `-I` 可直接看到。

### 实际结果

`resident-service` 不映射宿主机端口（`ports: []`），所有访问都来自内网，因此实例标识改从调用方日志取证：

```bash
# repair-service 日志中的 Feign 请求行（BASIC 级别）
docker logs resident-services-repair-service-1 2>&1 | grep -oE 'GET http://[0-9.]+:8081' | sort -u
GET http://172.20.0.3:8081
GET http://172.20.0.5:8081      # 两个不同实例 IP，证明分发生效

# 容器副本
docker ps --filter name=resident-services-resident-service --format '{{.Names}} {{.Status}}'
resident-services-resident-service-1 Up (healthy)
resident-services-resident-service-2 Up (healthy)
```

另外 resident-service 的 `GET /api/system/instance` 返回 `hostname#PID`、`/api/residents/*` 响应带 `X-Instance-Id` 头，便于在容器内自查。

截图：`screenshots/03-nacos-2-instances.png`、`screenshots/04-lb-cross-hostnames.png`。

## 4. 测试

### 单元测试（不连接真实 Nacos / MySQL）

`services/repair-service/src/test/java/.../ResidentClientTest.java`（6 条）：

| 测试 | 覆盖 |
| --- | --- |
| `feignNotFoundMapsToResourceNotFoundException` | Feign 404 → 业务 404 |
| `feign5xxMapsToServiceUnavailableException` | 5xx → 503 |
| `feignFallbackThrownDirectlyAsServiceUnavailable` | Fallback 语义隔离 |
| `feignRuntimeExceptionMapsToServiceUnavailable` | 无实例 / 超时 → 503 |
| `happyPathDoesNotThrow` | 正常调用不抛异常 |
| `feignFallbackIsolation` | Fallback 自身不访问 Feign 网络 |

执行：

```bash
cd services/repair-service
docker run --rm -v "${PWD}:/workspace" -v /root/.m2 \
  maven:3.9.16-eclipse-temurin-25 mvn -B test
# Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

截图：`screenshots/05-unit-tests-6-pass.png`。

### 端到端脚本

`scripts/e2e-test.sh` 从仓库根目录执行，检查 6 大类共 10+ 项：

```bash
bash scripts/e2e-test.sh
```

| 检查项 | 说明 |
| --- | --- |
| Nacos 控制台 + 3 服务健康 | 基础可用性 |
| Nacos 注册 3 服务 | 组 PERSONAL_GROUP |
| resident-service scale 到 2 实例 | 多实例准备 |
| 12 次跨服务报修 | Feign + LB 实际分发 |
| 业主不存在 → 404 | 错误语义保留 |
| 停一个实例 → 部分失败 | 无部分成功写脏数据 |
| 恢复 → 重新可用 | 自恢复验证 |

任一失败 `exit 1`。

截图：`screenshots/06-e2e-pass.png`。

## 5. 文档与截图清单

| 文件 | 内容 |
| --- | --- |
| `../service-communication.md` | 跨服务链路图 + 边界说明 |
| `screenshots/01-feign-client-ok.png` | repair-service Feign 调用成功日志 |
| `screenshots/02-fallback-log.png` | Fallback 触发日志（可在停止 resident 后获取） |
| `screenshots/03-nacos-2-instances.png` | Nacos 控制台 resident-service 显示 2 实例 |
| `screenshots/04-lb-cross-hostnames.png` | 连续请求命中不同实例（X-Instance-Id 或日志 host 变化） |
| `screenshots/05-unit-tests-6-pass.png` | `mvn test` 6/6 通过 |
| `screenshots/06-e2e-pass.png` | `bash scripts/e2e-test.sh` 全部 PASS |

## 6. 与 week-07 对比

| 项 | week-07 | week-08 |
| --- | --- | --- |
| 调用方式 | `DiscoveryClient.getInstances()` + `RestTemplate` 手动拼 URI | `@FeignClient` + `RestTemplate` / 自动负载均衡 |
| 服务发现 | 每次调用手动查，自己选 `instances.get(0)` | OpenFeign 委托 Spring Cloud LoadBalancer，默认轮询 |
| 错误处理 | 自己 catch RestTemplate 异常做映射 | Fallback 自动触发 + ResidentClient 做 FeignException → 业务异常映射 |
| 多实例 | 单实例，没测多实例 | 双实例 scale，验证 LB 实际分发 |
| 超时 | RestTemplate 2s/5s | Feign `connect-timeout` / `read-timeout` 等价 |
