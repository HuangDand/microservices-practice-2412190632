package com.zjgsu.hwy.system.web;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("application", "智能安全老旧小区管理平台", "status", "UP", "version", "0.0.1-SNAPSHOT");
    }
}
