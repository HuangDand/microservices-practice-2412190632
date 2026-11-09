package com.zjgsu.hwy.controller;

import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.service.ConflictException;
import com.zjgsu.hwy.service.EnrollmentService;
import com.zjgsu.hwy.service.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    private EnrollmentController controller;

    @BeforeEach
    void setUp() {
        controller = new EnrollmentController(enrollmentService);
    }

    @Test
    void enroll_成功返回201() {
        Enrollment saved = new Enrollment();
        saved.setId("enroll-001");
        when(enrollmentService.enroll("student-001", "course-001")).thenReturn(saved);

        ResponseEntity<?> response = controller.enroll(
                Map.of("studentId", "student-001", "courseId", "course-001"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void enroll_重复选课返回409() {
        when(enrollmentService.enroll(anyString(), anyString()))
                .thenThrow(new ConflictException("该学生已经选修此课程，不可重复选课"));

        ResponseEntity<?> response = controller.enroll(
                Map.of("studentId", "student-001", "courseId", "course-001"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void enroll_学生不存在返回404() {
        when(enrollmentService.enroll(anyString(), anyString()))
                .thenThrow(new NotFoundException("学生不存在"));

        ResponseEntity<?> response = controller.enroll(
                Map.of("studentId", "student-001", "courseId", "course-001"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void enroll_依赖不可用返回503() {
        when(enrollmentService.enroll(anyString(), anyString()))
                .thenThrow(new ResourceAccessException("调用学生服务失败: Connection refused"));

        ResponseEntity<?> response = controller.enroll(
                Map.of("studentId", "student-001", "courseId", "course-001"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void withdraw_依赖不可用返回503_本地记录不删() {
        doThrow(new ResourceAccessException("调用课程服务失败: Read timed out"))
                .when(enrollmentService).withdraw("enroll-001");

        ResponseEntity<?> response = controller.withdraw("enroll-001");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void withdraw_成功返回204() {
        ResponseEntity<?> response = controller.withdraw("enroll-001");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
