# 数据库设计：智能安全老旧小区管理平台

## 实体关系
一个业主（residents）可以提交多张报修工单（repair_tickets）；每张工单必须关联一个业主。关系为 residents.id 1:N repair_tickets.resident_id。

## residents（业主表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | 主键、自增 | 业主 ID |
| full_name | VARCHAR(80) | NOT NULL | 姓名 |
| phone | VARCHAR(30) | NOT NULL、唯一 | 联系电话 |
| building | VARCHAR(30) | NOT NULL | 楼栋 |
| unit_number | VARCHAR(30) | NOT NULL | 单元房号 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |

## repair_tickets（报修工单表）
| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | 主键、自增 | 工单 ID |
| title | VARCHAR(120) | NOT NULL | 标题 |
| description | VARCHAR(2000) | NOT NULL | 描述 |
| location | VARCHAR(120) | NOT NULL | 报修位置 |
| status | VARCHAR(20) | NOT NULL | OPEN / IN_PROGRESS / RESOLVED |
| resident_id | BIGINT | 外键、NOT NULL | 关联业主 |
| created_at | TIMESTAMP | NOT NULL | 创建时间 |
| updated_at | TIMESTAMP | NOT NULL | 更新时间 |

索引：工单状态、resident_id。业主手机号唯一，工单必须关联有效业主。JPA 根据实体生成表结构；正式生产环境建议改用 Flyway/Liquibase 管理数据库版本。

## Service 规则
1. 业主手机号不得重复。
2. 报修标题、描述和位置不能为空。
3. 状态只允许 OPEN -> IN_PROGRESS -> RESOLVED，禁止跳级和回退。
