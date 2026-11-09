package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.EnrollmentService;
import com.zjgsu.hwy.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // ==========新增这一行！查询全部选课 GET /api/enrollments ==========
    @GetMapping
    public List<Enrollment> findAll() {
        return enrollmentService.findAll();
    }

    // 选课：POST /api/enrollments/enroll
    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        String courseId = body.get("courseId");
        try {
            Enrollment enrollment = enrollmentService.enroll(studentId, courseId);
            return new ResponseEntity<>(enrollment, HttpStatus.CREATED);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 退课：DELETE /api/enrollments/{enrollmentId}
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> withdraw(@PathVariable String enrollmentId) {
        try {
            enrollmentService.withdraw(enrollmentId);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 查询某个学生的所有选课记录 GET /api/enrollments/student/{studentId}
    @GetMapping("/student/{studentId}")
    public List<Enrollment> getStudentEnroll(@PathVariable String studentId) {
        return enrollmentService.getStudentEnrollments(studentId);
    }
}
