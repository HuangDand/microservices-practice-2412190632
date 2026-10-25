# 数据库设计文档

## 1. 数据库说明

项目名称：智能安全老旧小区管理平台。

数据库：MySQL 8.4。

持久化框架：Spring Data JPA。

数据库主要用于保存居民信息和居民提交的报修工单，保证应用重启后仍能查询已保存的数据。

## 2. 数据表关系

本项目设计两张核心业务表：

- `resident`：保存居民基本信息。
- `repair_ticket`：保存居民提交的报修工单。

两张表为一对多关系：一个居民可以提交多张报修工单，每张报修工单关联一个居民。

```text
resident
  id (PK)
    |
    | 1 : N
    |
repair_ticket
  resident_id (FK)
```

## 3. 居民表 resident

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | 主键、自增 | 居民 ID |
| full_name | VARCHAR(100) | 非空 | 居民姓名 |
| phone | VARCHAR(20) | 非空 | 联系电话 |
| building | VARCHAR(100) | 非空 | 楼栋信息 |
| unit_number | VARCHAR(100) | 非空 | 单元及房间信息 |
| created_at | DATETIME | 非空 | 创建时间 |

主要约束：

- `id` 为主键。
- 居民姓名、联系电话、楼栋和房间信息不能为空。
- API 输入需要执行相应的参数校验。

## 4. 报修工单表 repair_ticket

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | 主键、自增 | 工单 ID |
| resident_id | BIGINT | 非空、外键 | 提交报修的居民 ID |
| description | VARCHAR(1000) | 非空 | 报修内容 |
| status | VARCHAR(30) | 非空 | 工单状态 |
| created_at | DATETIME | 非空 | 工单创建时间 |

主要约束：

- `id` 为主键。
- `resident_id` 外键引用 `resident.id`。
- `description` 不能为空。
- `status` 采用预定义状态，例如 `PENDING`、`PROCESSING` 和 `COMPLETED`。
- 工单创建时，初始状态为 `PENDING`。

## 5. 数据完整性

数据库通过主键保证记录标识的唯一性，通过外键保证报修工单关联的居民存在。非空约束用于避免缺失必要的业务字段。

在 Service 层还应校验提交报修的居民是否存在，并限制工单状态的合法流转。

## 6. 持久化实现

应用使用 Spring Data JPA 实现数据库访问，Repository 接口负责数据读写，Service 负责业务规则，Controller 负责 HTTP 请求和响应。

API 输入通过请求 DTO 承载，并进行必要的参数校验。

## 7. 验证方式

1. 启动 MySQL 和 Spring Boot 应用。
2. 通过 POST API 新增居民。
3. 通过 GET API 查询居民。
4. 在 MySQL 中检查对应记录是否保存。
5. 重启应用后再次查询，确认数据仍然存在。
6. 对报修工单执行相同的创建、查询和关联关系验证。

尚未实现或尚未运行验证的功能，应在实际完成后再记录为已完成。
