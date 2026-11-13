package com.zjgsu.hwy.repair.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 通过 OpenFeign + Nacos 服务名调用业主服务。
 * 服务名 resident-service 由 Spring Cloud LoadBalancer 解析成具体实例（默认轮询），
 * 不写死容器 IP。调用失败的映射统一在 {@link ResidentClient} 中处理。
 */
@FeignClient(
        name = "resident-service",
        path = "/api/residents"
)
public interface ResidentFeignClient {

    /**
     * 查询指定 id 的业主。
     * 业主不存在时下游返回 404（Feign 抛 FeignException.NotFound）。
     */
    @GetMapping("/{id}")
    ResidentExistsResponse exists(@PathVariable("id") Long id);

    /** 极简 DTO，只关心 id 是否存在；避免在 repair-service 重复定义 Resident 完整模型。 */
    record ResidentExistsResponse(Long id, String fullName) {}
}
