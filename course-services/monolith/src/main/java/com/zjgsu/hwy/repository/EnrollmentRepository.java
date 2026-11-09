package com.zjgsu.hwy.repository;

import com.zjgsu.hwy.model.Enrollment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EnrollmentRepository {
    private final Map<String, Enrollment> dataMap = new ConcurrentHashMap<>();

    public Enrollment save(Enrollment enrollment) {
        dataMap.put(enrollment.getId(), enrollment);
        return enrollment;
    }

    public Optional<Enrollment> findById(String id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public List<Enrollment> findAll() {
        return new ArrayList<>(dataMap.values());
    }

    // 按学生id查询所有选课记录
    public List<Enrollment> findByStudentId(String studentId) {
        return dataMap.values().stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    // 检查：该学生是否已经选了这门课
    public boolean existsByStudentIdAndCourseId(String studentId, String courseId) {
        return dataMap.values().stream()
                .anyMatch(e -> e.getStudentId().equals(studentId) && e.getCourseId().equals(courseId));
    }

    // 根据id删除选课记录
    public void deleteById(String id) {
        dataMap.remove(id);
    }
}
