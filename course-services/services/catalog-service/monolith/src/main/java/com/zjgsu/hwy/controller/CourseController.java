package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.service.CourseService;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // 创建课程
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Course course) {
        try {
            Course saved = courseService.createCourse(course);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (ConflictException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 查询单个课程
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            Course course = courseService.findById(id)
                    .orElseThrow(() -> new NotFoundException("课程不存在"));
            return ResponseEntity.ok(course);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 查询全部课程
    @GetMapping
    public List<Course> getAll() {
        return courseService.findAll();
    }

    // 修改课程
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Course updateInfo) {
        try {
            Course updated = courseService.updateCourse(id, updateInfo);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
