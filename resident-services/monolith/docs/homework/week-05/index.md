# 第 05 周：Docker 容器化

## 架构
- app：多阶段 Dockerfile 构建的 Spring Boot 应用。
- mysql：MySQL 8.4.11。
- community_mysql_data：命名数据卷。
- 应用通过 Compose 网络中的 `mysql:3306` 连接数据库。
- MySQL 健康检查通过后应用才启动。

## 启动
首次运行复制 `.env.example` 为 `.env` 并修改密码：
```powershell
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 app
docker compose logs --tail=100 mysql
```
不要提交真实 `.env`。

## API
```powershell
curl.exe -i http://localhost:8080/actuator/health
curl.exe -i http://localhost:8080/api/residents
```
创建业主和工单的请求示例见根目录 README。

## 持久化验证
1. 通过 API 创建业主和工单并记录 ID。
2. 查询记录。
3. 执行 `docker compose down`（不要加 `-v`）。
4. 执行 `docker compose up -d`。
5. 等待应用启动，再次查询相同 ID 并比较字段。
命名卷保存 MySQL 数据目录，普通 down 不会删除卷。

## 截图
将真实截图放到 `screenshots/`：镜像构建、`docker compose ps`、API 创建/查询、重建前后相同记录。
本文件是记录模板，运行后按实际结果更新。
