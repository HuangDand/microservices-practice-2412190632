package com.zjgsu.hwy.service;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.model.EnrollmentStatus;
import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.repository.CourseRepository;
import com.zjgsu.hwy.repository.EnrollmentRepository;
import com.zjgsu.hwy.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    //构造器注入
    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CourseRepository courseRepository,
                             StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    // =========新增这一行=========
    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    // 选课 enroll，严格按顺序4条规则
    public Enrollment enroll(String studentId, String courseId) {
        // 1. 查询学生与课程；任一不存在则拒绝
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("学生不存在"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("课程不存在"));

        // 2. 检查该学生是否已选同一门课；重复则拒绝
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("该学生已经选修此课程，不可重复选课");
        }

        // 3. 检查 enrolled < capacity；满员则拒绝
        if (course.getEnrolled() >= course.getCapacity()) {
            throw new ConflictException("课程已满，无法选课");
        }

        // 4. 创建选课记录，课程已选人数+1，保存记录和课程
        Enrollment enrollment = new Enrollment();
        enrollment.setId(UUID.randomUUID().toString());
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        course.enrollOne();
        courseRepository.save(course);
        return enrollmentRepository.save(enrollment);
    }

    // 退课 withdraw
    public void withdraw(String enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("选课记录不存在"));
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new NotFoundException("课程不存在"));

        course.withdrawOne();
        courseRepository.save(course);
        enrollmentRepository.deleteById(enrollmentId);
    }

    // 根据学生id查询选课列表
    public List<Enrollment> getStudentEnrollments(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }
}
