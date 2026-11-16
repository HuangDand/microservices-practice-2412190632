package com.zjgsu.hwy;

import com.zjgsu.hwy.model.Account;
import com.zjgsu.hwy.model.AccountRole;
import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.repository.AccountRepository;
import com.zjgsu.hwy.repository.StudentRepository;
import com.zjgsu.hwy.service.StudentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataLoader implements CommandLineRunner {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String studentPassword;
    private final String adminPassword;

    public DataLoader(StudentService studentService,
                      StudentRepository studentRepository,
                      AccountRepository accountRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.student-password:}") String studentPassword,
                      @Value("${app.seed.admin-password:}") String adminPassword) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentPassword = studentPassword;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        loadStudent("2026001", "小明", "计算机科学");
        loadStudent("2026002", "小红", "软件工程");
        String studentId = studentRepository.findByStudentNumber("2026001")
                .map(Student::getId)
                .orElse(null);
        loadAccount("student1", AccountRole.STUDENT, studentId, studentPassword);
        loadAccount("admin", AccountRole.ADMIN, null, adminPassword);
        System.out.println("===== user-service 预加载学生与账号数据完成 =====");
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

    private void loadAccount(String username, AccountRole role, String studentId, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            System.out.println("未设置 " + username + " 的初始化密码，跳过账号创建");
            return;
        }
        if (accountRepository.findByUsername(username).isPresent()) {
            return;
        }
        Account account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setStudentId(studentId);
        accountRepository.save(account);
    }
}
