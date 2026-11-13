package com.zjgsu.hwy.system.web;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 系统状态 + 实例自标识。
 * /api/system/instance 返回当前 Pod 的 hostname 和 PID，
 * 用于 repair-service Feign 多实例负载均衡验证（每次请求会由不同实例处理）。
 */
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final String instanceId = initInstanceId();

    private static String initInstanceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            String pid  = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return host + "#" + pid;
        } catch (Exception e) {
            return "unknown#0";
        }
    }

    /** 应用健康元数据。 */
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "application", "resident-service",
                "status", "UP",
                "instanceId", instanceId,
                "version", "0.0.1-SNAPSHOT");
    }

    /** 返回当前实例的唯一标识，用于 LB 验证。 */
    @GetMapping("/instance")
    public InstanceInfo instance() {
        return new InstanceInfo(instanceId);
    }

    /** 同时放到 /api/residents/{id} 的响应头里，让调用方 Feign 也能拿到实例标识。 */
    public String currentInstanceId() {
        return instanceId;
    }

    public record InstanceInfo(String instanceId) {}
}
