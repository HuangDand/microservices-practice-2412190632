# resident-services

第 06 周拆分的社区微服务工程。第 07 周接入 Nacos 注册中心；第 08 周把报修服务对业主服务的调用改为 **OpenFeign + Spring Cloud LoadBalancer**，业主服务以 2 个实例运行，按服务名轮询分发。

## 服务清单

| 服务名（Nacos） | 模块 | 容器端口 | 宿主机端口 | 数据库（宿主机端口） |
| --- | --- | --- | --- | --- |
| `resident-service` | `services/resident-service` | 8081 | 不映射（仅内网） | `resident_db`（3311） |
| `repair-service` | `services/repair-service` | 8082 | 8092 | `repair_db`（3312） |
| `notice-service` | `services/notice-service` | 8083 | 8093 | `notice_db`（3313） |
| `nacos` | `nacos/nacos-server:v3.2.4` | 8848（服务端）/ 9848（SDK）/ 8080（控制台） | 18848 / 19848 / 18081 | `nacos-data` 数据卷 |

三个服务均注册到默认命名空间，分组固定为 `PERSONAL_GROUP`。

`resident-service` **不映射宿主机端口**：它只被 `repair-service` 通过 Nacos 服务名 + Docker 内部网络调用，因此 `--scale` 多实例时不会出现宿主机端口冲突。需要直连它调试时，可临时用 `docker compose exec` 进入容器，或查看容器健康状态：

```bash
docker ps --filter name=resident-services-resident-service --format '{{.Names}} {{.Status}}'
```

## 前置条件

- Docker 与 Docker Compose
- 根目录提供 `.env`（已被 `.gitignore` 排除，不入库）

## 首次启动

```bash
# 1. 准备本地凭据（PowerShell: Copy-Item .env.example .env）
cp .env.example .env
# 编辑 .env：至少替换 NACOS_PASSWORD 与 NACOS_AUTH_TOKEN（Base64，原文 >= 32 字节）
# 生成 Token 示例：openssl rand -base64 32

# 2. 先启动数据库与 Nacos
docker compose up -d resident-db repair-db notice-db nacos

# 3. 等待控制台可访问（http://localhost:18081），首次初始化管理员密码
#    密码值取自 .env 的 NACOS_PASSWORD
curl -X POST "http://localhost:18848/nacos/v3/auth/user/admin" -d "password=$NACOS_PASSWORD"

# 4. 启动并构建三个业务服务
docker compose up -d --build
```

管理员密码只需初始化一次；若删除 `nacos-data` 数据卷，需要重新初始化。

## 查看注册与调用

- Nacos 控制台：<http://localhost:18081>，登录后进入“服务管理 -> 服务列表”，分组 `PERSONAL_GROUP` 下应有 `resident-service`、`repair-service`、`notice-service` 及其实例 IP/端口。本机 18080/8848 已被其他工程占用，因此使用 18081/18848 映射。
- 三个服务的健康端点（`resident-service` 无宿主机端口，用容器健康状态）：

```bash
docker ps --filter name=resident-services-resident-service --format '{{.Names}} {{.Status}}'
curl http://localhost:8092/actuator/health
curl http://localhost:8093/actuator/health
```

- 跨服务业务流程（报修服务经 OpenFeign 校验业主后创建工单）：

```bash
# 业主服务无宿主机端口，前置住户数据直接写入 resident_db（脚本里也是这么做的）
RID="$(docker compose exec -T resident-db sh -c 'mysql -N -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" 2>/dev/null -e "INSERT INTO residents (full_name,phone,building,unit_number,created_at) VALUES (\"张三\",\"13800138000\",\"1号楼\",\"301\",NOW(3)); SELECT LAST_INSERT_ID();"' | tail -n1 | tr -d '\r')"
echo "residentId=${RID}"

# repair-service 通过 Feign + LoadBalancer 调用 resident-service
curl -X POST -H "Content-Type: application/json" -d "{
  \"residentId\":${RID},
  \"title\":\"墙面渗水\",
  \"description\":\"卫生间墙面渗水\",
  \"location\":\"1号楼301\"
}" http://localhost:8092/api/repair-tickets
```

报修服务不再读取 `RESIDENT_SERVICE_URL`，也不再自己调用 `DiscoveryClient`：`ResidentFeignClient` 以服务名 `resident-service` 声明依赖，由 Spring Cloud LoadBalancer 在注册实例间分发。无可用实例或调用失败时由 `ResidentFeignFallback` 抛出 `ServiceUnavailableException` 返回 `503`，业主不存在保留 `404`。

## 冒烟测试

在仓库根目录执行：

```bash
bash scripts/smoke-test.sh
```

脚本依次检查 Nacos 控制台、三个服务健康（`resident-service` 走容器健康状态）、住户数据准备、经 OpenFeign 的报修创建、业主不存在 404、公告服务读写；任一检查失败即以非零状态退出。

## 多实例与端到端验证（第 08 周）

```bash
# 业主服务扩到 2 个实例（不映射宿主机端口，因此不会端口冲突）
docker compose up -d --no-recreate --scale resident-service=2 resident-service

# 端到端验证：Nacos 注册、双实例、12 次跨服务报修、负载均衡命中不同实例、
# 404 语义、停 1 个副本后仍可服务、恢复后 201
export NACOS_PASSWORD="$(grep -E '^NACOS_PASSWORD=' .env | cut -d= -f2-)"
bash scripts/e2e-test.sh
```

脚本通过 `repair-service` 日志里的 Feign 请求行（`GET http://<实例IP>:8081/...`）统计本轮命中的不同实例地址，命中 ≥2 个即判定负载均衡生效。还原为单实例：`docker compose up -d --no-recreate --scale resident-service=1 resident-service`。

## 服务下线与恢复验证

```bash
# 停掉全部业主服务副本
docker compose stop resident-service

# 此时创建报修：无可用实例 → Fallback → 503
curl -i -X POST -H "Content-Type: application/json" -d '{
  "residentId":1,"title":"下线验证","description":"依赖不可用","location":"1号楼"
}' http://localhost:8092/api/repair-tickets

# 恢复
docker compose start resident-service
docker ps --filter name=resident-services-resident-service --format '{{.Names}} {{.Status}}'
# 待容器 healthy 且 Nacos 实例列表恢复后重试创建报修，期望 201
```

注意：`actuator/health` 反映应用（含数据库）健康；Nacos 实例是否在线由 SDK 心跳维持。健康为 `DOWN` 不会自动让 Nacos 摘除实例，反之实例注册正常也不代表业务健康。

## 单服务测试（不依赖真实 Nacos/MySQL）

```bash
# 进入任一 services/<服务名>/ 后执行；测试使用 Mock 的 DiscoveryClient 与 RestTemplate
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.16-eclipse-temurin-25 mvn -B test
```

示例（报修服务，含空实例列表、404/503 映射测试）：

```bash
cd services/repair-service
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.16-eclipse-temurin-25 mvn -B test
```

## 环境变量

| 变量 | 作用 |
| --- | --- |
| `NACOS_SERVER_ADDR` | Nacos 服务端地址，Compose 中为 `nacos:8848` |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | Nacos 鉴权账号（管理员密码由首次初始化设置） |
| `NACOS_GROUP` | 注册与查询分组，默认 `PERSONAL_GROUP` |
| `NACOS_AUTH_TOKEN` | Nacos Token 签名密钥（Base64，原文 >= 32 字节） |
| `NACOS_AUTH_IDENTITY_KEY` / `NACOS_AUTH_IDENTITY_VALUE` | Nacos 服务端互通身份 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | 各服务独立数据库连接 |
| `RESIDENT_DB_*` / `REPAIR_DB_*` / `NOTICE_DB_*` | 各数据库密码（仅本机 `.env`） |
