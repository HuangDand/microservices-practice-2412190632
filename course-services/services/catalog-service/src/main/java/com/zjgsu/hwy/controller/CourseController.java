package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.CourseService;
import com.zjgsu.hwy.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courses;

    public CourseController(CourseService courses) {
        this.courses = courses;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Course course) {
        try {
            Course saved = courses.createCourse(course);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (ConflictException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            Course course = courses.findById(id)
                    .orElseThrow(() -> new NotFoundException("课程不存在"));
            return ResponseEntity.ok(course);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public List<Course> getAll() {
        return courses.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Course updateInfo) {
        try {
            Course updated = courses.updateCourse(id, updateInfo);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/seats")
    public ResponseEntity<Void> occupySeat(@PathVariable String id) {
        try {
            courses.occupySeat(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/{id}/seats")
    public ResponseEntity<Void> releaseSeat(@PathVariable String id) {
        try {
            courses.releaseSeat(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
