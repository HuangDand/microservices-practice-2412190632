package com.zjgsu.hwy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "student_number"))
public class Student {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "student_number", unique = true, nullable = false, length = 32)
    private String studentNumber;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 128)
    private String major;

    public Student() {}

    public Student(String id, String studentNumber, String name, String major) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.name = name;
        this.major = major;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
}
