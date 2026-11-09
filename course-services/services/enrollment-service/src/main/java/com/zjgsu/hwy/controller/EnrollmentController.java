package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.EnrollmentService;
import com.zjgsu.hwy.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
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

    @GetMapping
    public List<Enrollment> findAll() {
        return enrollmentService.findAll();
    }

    @PostMapping
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        String courseId = body.get("courseId");
        try {
            Enrollment enrollment = enrollmentService.enroll(studentId, courseId);
            return new ResponseEntity<>(enrollment, HttpStatus.CREATED);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> withdraw(@PathVariable String enrollmentId) {
        try {
            enrollmentService.withdraw(enrollmentId);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}")
    public List<Enrollment> getStudentEnroll(@PathVariable String studentId) {
        return enrollmentService.getStudentEnrollments(studentId);
    }
}
