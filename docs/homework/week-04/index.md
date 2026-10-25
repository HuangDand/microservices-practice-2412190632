
# 第 4 周作业：业务建模与持久化

## 一、作业目标

在第 3 周 Spring Boot 工程的基础上，围绕“智能安全老旧小区管理平台”，实现居民提交报修工单、物业处理工单、居民查询工单状态的完整业务流程。

本周重点包括业务建模、REST API、Service 业务规则、JPA Repository 持久化和测试验证。

## 二、业务建模

### 1. 核心业务流程

本周选择“居民报修与物业处理”作为核心业务流程。

流程如下：

1. 居民提交报修请求。
2. 系统校验居民和报修内容。
3. 系统创建报修工单并保存。
4. 物业更新工单处理状态。
5. 居民查询工单及处理进度。

### 2. 核心实体

- `Resident`：保存居民姓名、联系电话、楼栋和房间等信息。
- `RepairTicket`：保存报修描述、工单状态、创建时间以及关联居民。

一个居民可以关联多张报修工单，一张工单只关联一个居民。

### 3. Service 业务规则

- 报修工单必须关联已存在的居民。
- 报修描述不能为空。
- 新建工单默认处于 `PENDING` 状态。
- 工单状态只能按照规定的流程流转。

这些规则应由 Service 层检查，并通过测试或接口请求验证。

## 三、实现思路

采用分层设计：

- Entity：定义居民和报修工单实体。
- Repository：负责实体的数据库读写。
- Service：负责创建工单、检查业务规则和更新状态。
- Controller：提供 REST API。
- DTO：定义请求和响应的数据结构。

数据库采用 MySQL，使用 Spring Data JPA 实现持久化。

## 四、运行与验证

### 1. 启动项目

在项目根目录执行：

```bash
docker compose up -d --build
```

查看服务状态：

```bash
docker compose ps
```

查看应用日志：

```bash
docker compose logs -f app
```

如果应用服务在 `compose.yaml` 中使用了其他名称，请将 `app` 替换为实际服务名。

### 2. 健康检查

访问：

```text
http://localhost:8080/actuator/health
```

预期结果：返回 JSON，状态字段为 `UP`。

### 3. 已验证的居民接口

目前已手动验证：

- `POST /api/residents`：新增居民，返回 `201 Created`。
- `GET /api/residents/{id}`：按 ID 查询居民，返回 `200 OK`。
- `GET /api/residents`：查询居民列表，返回 `200 OK`。
- 新增居民时必填字段为空，返回 `400 Bad Request`。

### 4. 报修流程验证

待报修工单功能实现后，应进一步验证：

- 居民提交报修后生成工单。
- 不存在的居民不能提交报修。
- 报修描述为空时请求被拒绝。
- 工单初始状态正确。
- 工单状态更新符合业务规则。
- 工单数据能够从 MySQL 读取。

尚未完成或未实际运行的测试，不应记录为已通过。

## 五、测试与证明材料

建议保存以下截图或日志：

1. Docker Compose 服务正常运行的截图。
2. Actuator 健康检查返回 `UP` 的截图。
3. 创建报修工单成功的请求和响应截图。
4. 查询工单详情的截图。
5. 业务规则校验失败的响应截图。
6. 自动化测试运行结果截图。

每份截图应附上接口地址、请求参数、预期结果和实际结果。

## 六、本周结论

目前已完成应用启动、MySQL 连接及居民信息基础接口的手动验证。接下来需要结合报修工单实体，补齐 Service 业务规则、相关 REST API、持久化验证和自动化测试，形成可重复验证的完整业务流程。
## 单体应用实现

### 实现思路

本项目沿用第 3 周的 Spring Boot 工程，在原有基础包 `com.zjgsu.hwy` 下组织业务代码，采用 Controller、Service、Repository、Model 分层结构。Controller 负责接收 HTTP 请求和返回响应，Service 负责业务规则检查，Repository 负责数据库读写。

项目围绕居民信息管理和居民报修流程设计业务模型，使用 Spring Data JPA 和 MySQL 实现持久化。

### 运行命令

在项目根目录启动应用：

```bash
docker compose up -d --build
```

查看容器状态：

```bash
docker compose ps
```

查看应用日志：

```bash
docker compose logs -f app
```

健康检查地址：

```text
http://localhost:8080/actuator/health
```

### 接口验证

1. 使用 `POST /api/residents` 创建居民，检查是否返回 `201 Created`。
2. 使用 `GET /api/residents` 查询居民列表，检查是否返回 `200 OK`。
3. 使用 `GET /api/residents/{id}` 查询指定居民。
4. 使用 `POST /api/repair-tickets` 创建报修工单，检查居民存在性校验及工单初始状态。

目前已经验证居民新增、列表查询、按 ID 查询和参数校验。报修工单接口及其业务规则应在实际实现并运行验证后，再记录为完成。

### 截图材料

将实际运行结果截图保存到 `docs/homework/week-04/screenshots/`，建议包括：

- 应用启动及健康检查结果。
- 居民新增接口的成功响应。
- 居民列表或指定居民查询结果。
- 报修工单创建成功的响应。
- 业务规则校验失败的响应。

每张截图应能够对应具体接口、请求参数和实际返回结果。

## 数据库持久化

### 实现思路

项目采用 Spring Data JPA 作为持久化框架，使用 Repository 接口负责数据库读写，并通过 Service 层组织业务逻辑。Controller 负责接收 HTTP 请求和返回响应，API 输入使用 DTO 或请求对象承载。

数据库设计围绕居民信息和报修工单展开。居民表保存基本信息，报修工单表通过居民 ID 关联居民，形成一对多关系。

### 数据库配置

数据库名称：`community_safety`。

数据库类型：MySQL 8.4。

应用通过 JDBC 连接 MySQL，数据库连接信息由应用配置文件和环境变量提供。提交到公开仓库的配置中不应包含真实密码。

### 运行与验证

启动应用后，使用以下命令新增居民：

```bat
curl.exe -i -X POST "http://localhost:8080/api/residents" -H "Content-Type: application/json" -d "{\"fullName\":\"张三\",\"phone\":\"13800000001\",\"building\":\"3号楼\",\"unitNumber\":\"2单元501\"}"
```

查询居民列表：

```bat
curl.exe -i "http://localhost:8080/api/residents"
```

通过 MySQL 查询居民记录，确认 API 创建的数据已保存至数据库。重启应用后再次查询，可进一步验证数据持久化。

### 当前验证结果

已验证应用成功连接 MySQL，并通过 HTTP 请求完成居民新增、按 ID 查询和列表查询。必填字段为空时，接口能够返回参数校验错误。

第二张业务表、对应 Repository、测试数据准备脚本及报修工单持久化验证，需要以实际实现和运行结果为准。

### 证明材料

将实际运行截图保存到 `docs/homework/week-04/screenshots/`，建议包括：

1. 应用成功连接 MySQL 的日志。
2. 新增居民接口的成功响应。
3. MySQL 查询居民记录的结果。
4. 重启应用后查询到原有数据的结果。
5. 测试数据准备或自动化测试执行结果。
