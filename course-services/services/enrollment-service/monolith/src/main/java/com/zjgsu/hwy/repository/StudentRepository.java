package com.zjgsu.hwy.repository;

import com.zjgsu.hwy.model.Student;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class StudentRepository {
    private final Map<String, Student> dataMap = new ConcurrentHashMap<>();

    public Student save(Student student) {
        dataMap.put(student.getId(), student);
        return student;
    }

    public Optional<Student> findById(String id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public List<Student> findAll() {
        return new ArrayList<>(dataMap.values());
    }

    // 根据学号查询，校验学号唯一
    public Optional<Student> findByStudentNumber(String studentNumber) {
        return dataMap.values().stream()
                .filter(s -> s.getStudentNumber().equals(studentNumber))
                .findFirst();
    }
}
