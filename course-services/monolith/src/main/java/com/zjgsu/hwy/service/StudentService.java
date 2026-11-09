package com.zjgsu.hwy.service;

import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student createStudent(Student student) {
        // 校验学号不能重复
        if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
            throw new ConflictException("该学号已存在，不能重复");
        }
        student.setId(UUID.randomUUID().toString());
        return studentRepository.save(student);
    }

    public Optional<Student> findById(String id) {
        return studentRepository.findById(id);
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }
}
