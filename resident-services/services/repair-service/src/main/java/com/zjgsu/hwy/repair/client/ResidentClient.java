package com.zjgsu.hwy.repair.client;

import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.common.ServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 业务层对业主服务的门面：由 OpenFeign + Nacos + LoadBalancer 完成服务发现与负载均衡。
 * 这里集中做“调用失败”的映射，保证不把依赖故障当成业务成功：
 * <ul>
 *   <li>下游 404：业主不存在，保留原有业务语义</li>
 *   <li>5xx / 网络异常 / 无可用实例：统一 503，并给出清晰错误信息</li>
 * </ul>
 */
@Component
public class ResidentClient {
    public static final String SERVICE_ID = "resident-service";
    private static final Logger log = LoggerFactory.getLogger(ResidentClient.class);

    private final ResidentFeignClient feign;

    public ResidentClient(ResidentFeignClient feign) {
        this.feign = feign;
    }

    /** 校验业主存在；不存在抛 404，无可用实例或调用失败抛 503。 */
    public void ensureExists(Long residentId) {
        try {
            ResidentFeignClient.ResidentExistsResponse resp = feign.exists(residentId);
            log.info("Feign 调用 resident-service 成功, residentId={}", resp != null ? resp.id() : residentId);
        } catch (FeignException.NotFound e) {
            // 业主不存在：404 是显式业务响应，必须保留
            throw new ResourceNotFoundException("业主不存在: " + residentId);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (FeignException e) {
            // 下游有实例但返回 5xx 等异常
            throw new ServiceUnavailableException(
                    "调用业主服务失败: HTTP " + e.status() + ", residentId=" + residentId, e);
        } catch (RuntimeException e) {
            // LoadBalancer 无可用实例、连接被拒、读超时等
            log.warn("业主服务调用失败: {}", e.getMessage());
            throw new ServiceUnavailableException(
                    "业主服务暂时不可用（" + SERVICE_ID + "），请稍后重试", e);
        }
    }
}
