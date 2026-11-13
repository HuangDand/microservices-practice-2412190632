package com.zjgsu.hwy.system.web;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("application", "repair-service", "status", "UP", "version", "0.0.1-SNAPSHOT");
    }
}
