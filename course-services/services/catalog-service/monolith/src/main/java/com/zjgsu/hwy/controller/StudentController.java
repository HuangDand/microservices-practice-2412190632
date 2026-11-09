package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.NotFoundException;
import com.zjgsu.hwy.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // 创建学生
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Student student) {
        try {
            Student saved = studentService.createStudent(student);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (ConflictException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 根据id查询学生
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            Student student = studentService.findById(id)
                    .orElseThrow(() -> new NotFoundException("学生不存在"));
            return ResponseEntity.ok(student);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 查询所有学生
    @GetMapping
    public List<Student> getAll() {
        return studentService.findAll();
    }
}
