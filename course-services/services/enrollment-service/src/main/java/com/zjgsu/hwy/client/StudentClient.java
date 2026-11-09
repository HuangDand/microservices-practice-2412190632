package com.zjgsu.hwy.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service")
public interface StudentClient {

    @GetMapping("/api/students/{id}")
    Map<String, Object> findById(@PathVariable("id") String id);
}
