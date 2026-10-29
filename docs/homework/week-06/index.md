## 代码拆分
### 实现思路
按照业务边界将单体报修系统拆分为 **resident-service（住户服务）**、**repair-service（报修服务）** 两个独立微服务。
每个服务都拥有独立的 pom.xml、独立 application.yml 配置文件，各自包含至少一个 Controller、一个 Service 业务类以及配套单元测试；单元测试使用 H2 内存数据库，不需要启动 Docker MySQL 就可以执行。

跨服务业务流程：repair-service 在新增报修记录之前，使用 RestTemplate 远程调用 resident-service，校验该住户编号是否真实存在。
- 住户服务地址写在 application.yml，支持环境变量覆盖；
- 配置 RestTemplate 连接超时、读取超时；
- 校验逻辑：住户不存在、resident-service 服务不可达、请求超时的时候，捕获异常，返回清晰错误信息，**不写入报修记录，避免脏数据**。

两个服务数据库相互独立，服务之间通过 HTTP 完成跨服务通信。

### 运行命令
```bash
# 单个服务单元测试
cd services/resident-service
./mvnw test

cd services/repair-service
./mvnw test

# 项目根目录，使用 Compose 一键启动整套微服务
docker compose up -d --build

# 查看容器运行状态
docker compose ps

# 调用报修接口，触发 RestTemplate 跨服务校验住户
curl -X POST http://localhost:8083/repairs \
-H "Content-Type: application/json" \
-d '{"residentId": 1, "content": "客厅灯光故障"}'
