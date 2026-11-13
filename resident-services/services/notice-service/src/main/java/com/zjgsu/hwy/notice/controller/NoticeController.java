package com.zjgsu.hwy.notice.controller;

import com.zjgsu.hwy.notice.dto.CreateNoticeRequest;
import com.zjgsu.hwy.notice.dto.UpdateNoticeRequest;
import com.zjgsu.hwy.notice.model.Notice;
import com.zjgsu.hwy.notice.service.NoticeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Notice> create(@Valid @RequestBody CreateNoticeRequest r) {
        Notice x = service.create(r);
        return ResponseEntity.created(URI.create("/api/notices/" + x.getId())).body(x);
    }

    @GetMapping
    public List<Notice> list(@RequestParam(required = false) String keyword) {
        return service.list(keyword);
    }

    @GetMapping("/{id}")
    public Notice get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public Notice update(@PathVariable Long id, @Valid @RequestBody UpdateNoticeRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
