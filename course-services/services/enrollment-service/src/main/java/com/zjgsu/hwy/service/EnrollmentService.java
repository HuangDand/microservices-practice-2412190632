package com.zjgsu.hwy.service;

import com.zjgsu.hwy.client.CatalogClient;
import com.zjgsu.hwy.client.StudentClient;
import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.model.EnrollmentStatus;
import com.zjgsu.hwy.repository.EnrollmentRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CatalogClient catalogClient;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentClient studentClient,
                             CatalogClient catalogClient) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentClient = studentClient;
        this.catalogClient = catalogClient;
    }

    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    public Enrollment enroll(String studentId, String courseId) {
        // 1. 本地重复选课检查（不发起远程调用，重复请求在任何远程访问前即返回 409）
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("该学生已经选修此课程，不可重复选课");
        }

        // 2. 校验学生
        Map<String, Object> student;
        try {
            student = studentClient.findById(studentId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new NotFoundException("学生不存在");
            }
            throw dependencyFailure("学生服务", e);
        }
        if (student == null || !studentId.equals(student.get("id"))) {
            throw new ResourceAccessException("学生服务返回无效响应");
        }

        // 3. 调用课程服务占用名额（课程不存在由 occupy 返回 404，满员返回 409）
        try {
            catalogClient.occupy(courseId);
        } catch (FeignException e) {
            if (e.status() == 409) {
                throw new ConflictException("课程已满，无法选课");
            }
            if (e.status() == 404) {
                throw new NotFoundException("课程不存在");
            }
            throw dependencyFailure("课程服务", e);
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setId(UUID.randomUUID().toString());
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        return enrollmentRepository.save(enrollment);
    }

    public void withdraw(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("选课记录不存在"));

        try {
            catalogClient.release(enrollment.getCourseId());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new NotFoundException("课程不存在，无法释放名额");
            }
            throw dependencyFailure("课程服务", e);
        }

        enrollmentRepository.deleteById(enrollmentId);
    }

    public List<Enrollment> getStudentEnrollments(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * 连接失败、读取超时、下游 5xx 统一视为依赖不可用（503），
     * 绝不把调用失败解释成“学生不存在”或“还有空位”。
     * 写调用超时后远端可能已执行，因此不做自动重试。
     */
    private ResourceAccessException dependencyFailure(String serviceName, FeignException e) {
        return new ResourceAccessException("调用" + serviceName + "失败: " + e.getMessage());
    }
}
