package com.zjgsu.hwy.repair.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.common.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 纯单元测试：Mock ResidentFeignClient，验证调用失败的映射。
 * 不连接真实 Nacos / MySQL / Feign 服务。
 */
class ResidentClientTest {

    private final ResidentFeignClient feign = mock(ResidentFeignClient.class);
    private final ResidentClient client = new ResidentClient(feign);

    private static Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "http://resident-service/api/residents/999",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    @Test
    void feignNotFoundMapsToResourceNotFoundException() {
        when(feign.exists(999L)).thenThrow(
                new FeignException.NotFound("[404] Resident not found", dummyRequest(), null, null));

        assertThrows(ResourceNotFoundException.class, () -> client.ensureExists(999L));
    }

    @Test
    void feign5xxMapsToServiceUnavailableException() {
        when(feign.exists(1L)).thenThrow(
                new FeignException.InternalServerError("[500] Resident service down", dummyRequest(), null, null));

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class, () -> client.ensureExists(1L));
        assertTrue(ex.getMessage().contains("调用业主服务失败"));
    }

    @Test
    void noInstanceMapsToServiceUnavailableException() {
        // LoadBalancer 无可用实例时抛 IllegalStateException
        when(feign.exists(1L)).thenThrow(
                new IllegalStateException("Load balancer does not contain an instance for the service resident-service"));

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class, () -> client.ensureExists(1L));
        assertTrue(ex.getMessage().contains("resident-service"));
    }

    @Test
    void timeoutsAndConnectionErrorsMapToServiceUnavailable() {
        when(feign.exists(1L)).thenThrow(new RuntimeException("Read timed out"));

        assertThrows(ServiceUnavailableException.class, () -> client.ensureExists(1L));
    }

    @Test
    void happyPathDoesNotThrow() {
        when(feign.exists(7L)).thenReturn(
                new ResidentFeignClient.ResidentExistsResponse(7L, "张三"));

        client.ensureExists(7L);
    }
}
