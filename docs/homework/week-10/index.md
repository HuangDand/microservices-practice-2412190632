# Week 10 · Nacos Config 配置中心

本周将个人微服务系统中的非敏感配置迁移到 Nacos Config，实现多服务集中配置管理、dev/test 多环境隔离和业务参数动态刷新，并通过自动化脚本验证配置确实被服务读取和使用。

## 1. 配置中心

### 实现思路

- 为 resident、repair、notice、gateway、auth 五个服务添加 `spring-cloud-starter-alibaba-nacos-config` 依赖。
- 在各服务的 `application.yaml` 中使用 `spring.config.import` 导入 Nacos 配置。
- 将日志级别、Feign 超时时间、JWT 过期时间和工单业务参数等非敏感配置迁移到 Nacos。
- 数据库密码、Nacos 密码和 JWT 签名密钥仍通过环境变量提供，不放入 Nacos。
- 配置源文件统一保存在 `config/nacos/<环境>/`，通过发布脚本写入 Nacos，保证部署过程可复现。
- 配置导入使用 `optional:` 前缀，避免 Nacos 暂时不可用时服务直接无法启动。

服务读取配置的格式为：

```text
optional:nacos:<服务名>-<APP_ENV>.yaml?group=PERSONAL_GROUP&refreshEnabled=true
```

### 运行命令

```bash
export NACOS_PASSWORD="你的Nacos密码"
bash scripts/nacos-config-init.sh all
docker compose up -d --build
```

### 验证结果

- 配置发布脚本成功发布 dev、test 两套配置，共 10 个 Data ID。
- Nacos 控制台可以查看五个服务的配置，Group 均为 `PERSONAL_GROUP`。
- `config-test.sh` 能够读取 dev/test 配置，并通过业务接口证明服务实际使用了 Nacos 中的参数。

![Nacos 配置列表](screenshots/01-nacos-config-list.png)

## 2. 多环境配置

### 实现思路

使用 `APP_ENV` 区分开发和测试环境，同一套服务代码根据环境变量读取不同的 Data ID。

Data ID 命名规则：

```text
<服务名>-<环境>.yaml
```

例如：

- `repair-service-dev.yaml`
- `repair-service-test.yaml`
- `resident-service-dev.yaml`
- `resident-service-test.yaml`

所有配置的 Group 均为 `PERSONAL_GROUP`，命名空间为 `public`。完整的 Data ID、Group 和用途记录见 [docs/configuration.md](../../configuration.md)。

### 环境差异

| 环境 | 主要用途 | 工单上限 | 日志级别 | Feign 读取超时 | JWT 有效期 |
| --- | --- | ---: | --- | ---: | ---: |
| dev | 本地开发 | 2 | DEBUG | 5000 ms | 120 分钟 |
| test | 自动测试与演示 | 5 | INFO | 3000 ms | 30 分钟 |

dev 环境使用更详细的日志，方便本地排查问题；test 环境使用独立的业务参数和较短的超时时间，便于自动化测试。

### 运行命令

```bash
# 使用开发环境
APP_ENV=dev docker compose up -d

# 使用测试环境
APP_ENV=test docker compose up -d
```

切换环境只需修改 `APP_ENV`，不需要修改服务代码或重新编写配置文件。

### 验证结果

配置发布脚本能够分别发布 dev 和 test 配置，`config-test.sh` 已成功读取：

- `repair-service-dev.yaml`
- `repair-service-test.yaml`

两套配置中的工单上限、日志级别、Feign 超时和 JWT 有效期均保持独立。

![多环境配置差异]

## 3. 动态配置验证

### 实现思路

选择业务参数 `repair.feature.max-open-tickets-per-resident` 作为动态观察对象，表示每个业主允许拥有的最大未完成工单数量。

服务通过 `@ConfigurationProperties` 绑定该参数，并使用 `@RefreshScope` 支持配置刷新。同时提供以下观察接口：

```text
GET /api/repair-tickets/config/feature
```

验证过程：

1. 读取当前工单上限，dev 环境初始值为 `2`。
2. 同一业主连续创建两个工单，均返回 HTTP 201。
3. 创建第三个工单，超过上限，返回 HTTP 409。
4. 通过 Nacos API 将上限从 `2` 修改为 `4`。
5. 不重启容器、不重新打包，轮询观察接口，直到读取到新值 `4`。
6. 同一业主再次创建工单，返回 HTTP 201。
7. 验证结束后将配置恢复为 `2`，避免影响其他测试。

### 运行命令

```bash
export NACOS_PASSWORD="你的Nacos密码"
bash scripts/config-test.sh
```

### 验证结果

```text
[PASS] 前 2 个工单全部创建成功
[PASS] 超出上限被拒绝 (HTTP 409)
[PASS] 配置热更新生效：上限 2 -> 4（未重启）
[PASS] 放宽上限后同一业主可继续创建 (HTTP 201)
[PASS] 配置已恢复为 2
PASS=12  FAIL=0
```

该结果证明配置变更不仅在 Nacos 中保存成功，而且已被运行中的服务读取并应用到实际业务逻辑，无需重启或重新打包。

![Nacos 修改配置]

![动态配置验证结果]

## 4. 安全配置

### 实现思路

- 真实 `.env` 文件由 `.gitignore` 排除，不提交到 Git。
- 仓库仅提供 `.env.example`，用于说明需要配置的环境变量。
- 数据库密码、Nacos 密码和 JWT 签名密钥不写入 Nacos 配置文件。
- JWT 密钥通过 `JWT_SECRET` 环境变量注入。
- 演示账号密码支持通过 `AUTH_ADMIN_PASSWORD` 和 `AUTH_RESIDENT_PASSWORD` 覆盖。
- Nacos 中只保存日志级别、超时时间、过期时间和业务参数等非敏感配置。

### 本地设置方法

```bash
cp .env.example .env
```

随后在本地 `.env` 中设置实际值：

```text
NACOS_PASSWORD=本地Nacos密码
JWT_SECRET=本地JWT签名密钥
DB_PASSWORD=本地数据库密码
AUTH_ADMIN_PASSWORD=本地管理员密码
AUTH_RESIDENT_PASSWORD=本地业主密码
APP_ENV=dev
```

README 中也说明了环境变量的设置方法，其他开发者可以根据 `.env.example` 创建自己的本地配置。

### 验证结果

- `.env` 已被 `.gitignore` 排除，不会出现在 Git 提交中。
- `.env.example` 只作为配置模板，不包含生产环境真实密钥。
- Nacos 配置中未保存数据库密码或 JWT 签名密钥。
- 服务能够通过环境变量正常获取敏感配置。

![环境变量示例]
