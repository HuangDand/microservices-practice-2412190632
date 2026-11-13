## 第 04 周：业务实现

环境：JDK 25、Spring Boot 4.0.x、Maven Wrapper。进入 `monolith/` 执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

默认端口 8080。

创建业主：
```bash
curl -i -X POST http://localhost:8080/api/residents -H "Content-Type: application/json" -d "{\"fullName\":\"张三\",\"phone\":\"13800000001\",\"building\":\"3号楼\",\"unitNumber\":\"2单元501\"}"
```

创建工单（将 residentId 改为真实业主 ID）：
```bash
curl -i -X POST http://localhost:8080/api/repair-tickets -H "Content-Type: application/json" -d "{\"title\":\"楼道灯损坏\",\"description\":\"三楼楼道灯不亮\",\"location\":\"3号楼三层\",\"residentId\":1}"
```

查询：
```bash
curl -i "http://localhost:8080/api/repair-tickets?status=OPEN"
```

## 第 05 周：Docker Compose

```powershell
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d --build
docker compose ps
curl.exe -i http://localhost:8080/actuator/health
```

编辑 `.env` 设置强密码，绝不提交真实 `.env`。停止但保留数据：`docker compose down`，不要用 `docker compose down -v` 验证持久化。

API：`POST/GET /api/residents`、`GET /api/residents/{id}`、`POST/GET /api/repair-tickets`、`GET /api/repair-tickets/{id}`、`PATCH /api/repair-tickets/{id}/status`、`GET /actuator/health`。

尚未实现：用户注册登录、角色权限、访客审批、公告、真实硬件告警、消息通知和微服务拆分。
