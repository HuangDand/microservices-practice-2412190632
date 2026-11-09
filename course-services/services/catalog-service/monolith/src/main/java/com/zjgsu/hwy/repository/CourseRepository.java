package com.zjgsu.hwy.repository;

import com.zjgsu.hwy.model.Course;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CourseRepository {
    private final Map<String, Course> dataMap = new ConcurrentHashMap<>();

    // 保存/更新
    public Course save(Course course) {
        dataMap.put(course.getId(), course);
        return course;
    }

    // 根据id查询
    public Optional<Course> findById(String id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    // 查询全部
    public List<Course> findAll() {
        return new ArrayList<>(dataMap.values());
    }

    // 根据课程编号code查询（用于校验code唯一）
    public Optional<Course> findByCode(String code) {
        return dataMap.values().stream()
                .filter(course -> course.getCode().equals(code))
                .findFirst();
    }
}
