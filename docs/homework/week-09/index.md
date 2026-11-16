1. API Gateway

新增独立工程`services/gateway-service` ，基于 Spring Cloud Gateway 5.0.0（WebFlux）与 Nacos 服务发现，统一对外端口 8080。配置了 7 条显式路由：`/api/auth/**` 放行给认证服务，`/api/repair-tickets/**` 、`/api/residents/**` 、`/api/notices/**` 分别转发到对应服务，其中写操作路由额外要求 ROLE_ADMIN。所有路由目标均使用`lb://服务名` 而非硬编码 IP，由 LoadBalancer 在 Nacos 注册实例间分发。健康检查入口保留 Gateway 自带的`/actuator/health` 。验证方法：访问`http://localhost:8080/actuator/gateway/routes` 返回 7 条路由定义，且`/actuator/health` 返回`{"status":"UP"}` 。

2. 认证服务

新增`services/auth-service` ，在用户相关服务中实现认证功能，监听 8084 且不映射宿主机端口，对外只能经网关访问。提供`POST /api/auth/login` 登录接口，校验账号密码后签发 HS256 JWT，claims 包含 sub、roles、iss、exp，有效期 120 分钟。系统内置 admin（ROLE_ADMIN）与 resident1（ROLE_RESIDENT）两种角色，由 CommandLineRunner 在首次启动时幂等写入。密码使用 Spring Security Crypto 的 BCryptPasswordEncoder 哈希后存储，数据库中只有`$2a$10$` 开头的哈希值，源码与配置文件均无明文密码。签名密钥通过环境变量`JWT_SECRET` 注入，Gateway 与认证服务共享同一份，仓库中不提交真实密钥。验证方法：登录成功返回 token 与 roles 字段，错误密码返回 401。

3. 网关鉴权

在 Gateway 中实现`JwtAuthFilter` （GlobalFilter，order = -100），采用默认拒绝策略：路由 metadata 标记`auth: permitAll` 的放行，其余路由未携带`Authorization: Bearer` 或验签失败一律返回 401；路由 metadata 声明`requiredRole` 而用户角色不匹配时返回 403。验签成功后向转发请求注入`X-Username` 、`X-Roles` 、`X-Gateway-Sign` 三个可信头，注入前先`remove` 客户端传入的同名头，因此伪造身份头会被覆盖而非透传。防止绕过网关直连的措施是：auth-service 与业务服务容器均不映射宿主机端口，唯一对外入口只有 Gateway 的 8080。验证方法：未登录访问受保护接口返回 401，携带有效 Token 返回 200，业主低权限 Token 写工单返回 403，管理员 Token 写工单返回 201。

4. 测试脚本

新增`scripts/auth-test.sh` ，从仓库根目录执行，全部请求打网关统一入口，覆盖作业要求的四类场景：未登录访问受保护接口应为 401、登录获取 Token 应为 200、携带 Token 访问受保护接口应为 200、低权限 Token 访问管理接口应为 403，另外补充管理员写操作应返回 201。脚本启动前轮询`/actuator/health` 等待网关就绪，用`assert_code` 函数按 HTTP 状态码判定，逐条输出 PASS/FAIL 并统计总数，失败时返回非零退出码，可重复运行且幂等。验证方法：执行`bash scripts/auth-test.sh` ，输出`PASS=10 FAIL=0` ，退出码为 0。
