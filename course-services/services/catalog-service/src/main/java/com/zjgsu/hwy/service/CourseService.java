package com.zjgsu.hwy.service;

import com.zjgsu.hwy.model.Course;
import com.zjgsu.hwy.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Course createCourse(Course course) {
        if (courseRepository.findByCode(course.getCode()).isPresent()) {
            throw new ConflictException("课程编号code已存在，不能重复");
        }
        course.setId(UUID.randomUUID().toString());
        course.setEnrolled(0);
        return courseRepository.save(course);
    }

    public Optional<Course> findById(String id) {
        return courseRepository.findById(id);
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Course updateCourse(String id, Course updateInfo) {
        Course oldCourse = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
        oldCourse.setTitle(updateInfo.getTitle());
        oldCourse.setCapacity(updateInfo.getCapacity());
        oldCourse.setInstructor(updateInfo.getInstructor());
        oldCourse.setSchedule(updateInfo.getSchedule());
        return courseRepository.save(oldCourse);
    }

    public void occupySeat(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
        if (course.isFull()) {
            throw new ConflictException("课程已满，无法选课");
        }
        course.enrollOne();
        courseRepository.save(course);
    }

    public void releaseSeat(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("课程不存在"));
        course.withdrawOne();
        courseRepository.save(course);
    }
}
