
package com.zjgsu.hwy;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.model.Instructor;
import com.zjgsu.hwy.model.ScheduleSlot;
import com.zjgsu.hwy.model.Student;
import com.zjgsu.hwy.service.CourseService;
import com.zjgsu.hwy.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final CourseService courseService;
    private final StudentService studentService;

    public DataLoader(CourseService courseService, StudentService studentService) {
        this.courseService = courseService;
        this.studentService = studentService;
    }

    @Override
    public void run(String... args) throws Exception {
        // ========== 预插入学生 ==========
        Student s1 = new Student();
        s1.setStudentNumber("2026001");
        s1.setName("小明");
        s1.setMajor("计算机科学");
        studentService.createStudent(s1);

        Student s2 = new Student();
        s2.setStudentNumber("2026002");
        s2.setName("小红");
        s2.setMajor("软件工程");
        studentService.createStudent(s2);


        // ========== 预插入课程 ==========
        Course c1 = new Course();
        c1.setCode("CS101");
        c1.setTitle("Java程序设计");
        c1.setCapacity(2);
        c1.setInstructor(new Instructor("T01", "王老师", "wang@test.com"));
        c1.setSchedule(new ScheduleSlot(DayOfWeek.MONDAY, LocalTime.of(8,0), LocalTime.of(10,0)));
        courseService.createCourse(c1);

        Course c2 = new Course();
        c2.setCode("CS102");
        c2.setTitle("数据库原理");
        c2.setCapacity(3);
        c2.setInstructor(new Instructor("T02", "李老师", "li@test.com"));
        c2.setSchedule(new ScheduleSlot(DayOfWeek.TUESDAY, LocalTime.of(10,0), LocalTime.of(12,0)));
        courseService.createCourse(c2);

        System.out.println("===== 预加载测试数据完成 =====");
        System.out.println("学生1学号：2026001  | 课程1编号：CS101（容量2）");
    }
}
