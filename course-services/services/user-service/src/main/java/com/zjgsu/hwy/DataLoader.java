package com.zjgsu.hwy;

import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final StudentService studentService;

    public DataLoader(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void run(String... args) throws Exception {
        loadStudent("2026001", "小明", "计算机科学");
        loadStudent("2026002", "小红", "软件工程");
        System.out.println("===== user-service 预加载学生数据完成 =====");
    }

    private void loadStudent(String number, String name, String major) {
        try {
            Student s = new Student();
            s.setStudentNumber(number);
            s.setName(name);
            s.setMajor(major);
            studentService.createStudent(s);
        } catch (Exception ignored) {
        }
    }
}
