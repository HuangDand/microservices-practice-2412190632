package com.zjgsu.hwy;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.model.Instructor;
import com.zjgsu.hwy.model.ScheduleSlot;
import com.zjgsu.hwy.service.CourseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final CourseService courseService;

    public DataLoader(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public void run(String... args) throws Exception {
        loadCourse("CS101", "Java程序设计", 2, "T01", "王老师", "wang@test.com",
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0));
        loadCourse("CS102", "数据库原理", 3, "T02", "李老师", "li@test.com",
                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));
        System.out.println("===== catalog-service 预加载课程数据完成 =====");
    }

    private void loadCourse(String code, String title, int capacity,
                            String tid, String tname, String temail,
                            DayOfWeek day, LocalTime start, LocalTime end) {
        try {
            Course c = new Course();
            c.setCode(code);
            c.setTitle(title);
            c.setCapacity(capacity);
            c.setInstructor(new Instructor(tid, tname, temail));
            c.setSchedule(new ScheduleSlot(day, start, end));
            courseService.createCourse(c);
        } catch (Exception ignored) {
        }
    }
}
