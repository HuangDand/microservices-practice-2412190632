## 服务拆分设计
### 实现思路
按照业务边界对原有选课单体系统进行拆分，划分三个独立微服务，数据隔离、独立部署：
1. **user-service（8081）**：负责学生信息管理，独立数据库 `user_db`，模型：Student
2. **catalog-service（8082）**：课程目录服务，负责课程、教师、排课信息，独立数据库 `catalog_db`，模型：Course、Instructor、ScheduleSlot
3. **enrollment-service（8083）**：选课记录服务，负责选课、退课业务，独立数据库 `enrollment_db`，模型：Enrollment

拆分原则：
- 数据跟着业务走，每个服务独占自己的数据库，不共享库；
- 服务之间通过 HTTP 远程调用通信；
- 拆分后会引入分布式数据一致性、网络超时、服务故障等新问题；
- 使用 Docker Compose 统一编排所有服务与 MySQL 容器，端口互不冲突。

### 运行命令
```bash
# 1. 进入项目根目录
cd course-services

# 2. 构建并启动整套微服务容器
docker compose up -d --build

# 3. 查看容器运行状态
docker compose ps

# 4. 单元测试（进入对应服务模块目录执行，使用H2内存库，无需启动Docker）
cd user-service
./mvnw test

cd catalog-service
./mvnw test

cd enrollment-service
./mvnw test
