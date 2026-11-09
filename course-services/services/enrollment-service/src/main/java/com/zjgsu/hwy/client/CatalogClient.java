package com.zjgsu.hwy.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/courses/{id}")
    Map<String, Object> findById(@PathVariable("id") String id);

    @PostMapping("/api/courses/{id}/seats")
    void occupy(@PathVariable("id") String id);

    @DeleteMapping("/api/courses/{id}/seats")
    void release(@PathVariable("id") String id);
}
