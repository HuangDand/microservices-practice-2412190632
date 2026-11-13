package com.zjgsu.hwy.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @PostMapping("/api/courses/{id}/seats")
    void occupy(@PathVariable("id") String id);

    @DeleteMapping("/api/courses/{id}/seats")
    void release(@PathVariable("id") String id);
}
