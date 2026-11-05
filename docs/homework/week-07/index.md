Nacos 以 standalone 单机模式初始化，服务向 Nacos 8848 端口完成注册；容器内服务间通过 `nacos:8848` 访问注册中心，宿主机使用映射端口 18081 访问 Nacos 控制台，微服务之间调用使用容器内部实例端口。
本次业务流程为报修工单提交，repair-service 作为调用方，在 PERSONAL_GROUP 分组下，通过 Nacos 拉取 resident-service 住户服务实例，远程校验住户是否存在，校验通过后持久化报修记录，跨服务调用无异常，业务流程正常完成。
