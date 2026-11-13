# 第 04 周：业务流程、REST API 与数据库

## 选题与流程
智能安全老旧小区管理平台。本周实现“业主提交报修—保存工单—查询工单—管理员推进状态”。

## 业务建模
- Resident：业主，手机号唯一。
- RepairTicket：报修工单，必须关联一位业主。
- 一位业主可提交多张工单。
- 状态流转规则：OPEN -> IN_PROGRESS -> RESOLVED。

## API
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/residents` | 创建业主 |
| GET | `/api/residents` | 业主列表 |
| GET | `/api/residents/{id}` | 业主详情 |
| POST | `/api/repair-tickets` | 创建报修工单 |
| GET | `/api/repair-tickets` | 查询工单，可按 status/residentId 过滤 |
| GET | `/api/repair-tickets/{id}` | 工单详情 |
| PATCH | `/api/repair-tickets/{id}/status` | 更新工单状态 |
| GET | `/actuator/health` | 健康检查 |

## 测试
进入 monolith 后运行 `.\mvnw.cmd test`。测试使用 H2，不要求本地 MySQL 启动。以真实终端输出为准填写结果。

## curl 示例
创建业主：
```bash
curl -i -X POST http://localhost:8080/api/residents -H "Content-Type: application/json" -d "{\"fullName\":\"张三\",\"phone\":\"13800000001\",\"building\":\"3号楼\",\"unitNumber\":\"2单元501\"}"
```
创建工单（residentId 替换为实际 ID）：
```bash
curl -i -X POST http://localhost:8080/api/repair-tickets -H "Content-Type: application/json" -d "{\"title\":\"楼道灯损坏\",\"description\":\"三楼楼道灯不亮\",\"location\":\"3号楼三层\",\"residentId\":1}"
```
查询工单：
```bash
curl -i "http://localhost:8080/api/repair-tickets?status=OPEN"
```

## 截图
保存真实截图到 `screenshots/`：测试通过、创建业主和工单、查询及非法状态响应。请本机执行后再填写实际结果。
