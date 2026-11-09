package com.zjgsu.hwy.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "code"))
public class Course {

    @Id
    @Column(length = 64)
    private String id;

    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String title;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "instructor_id", length = 64)),
            @AttributeOverride(name = "name", column = @Column(name = "instructor_name", length = 64)),
            @AttributeOverride(name = "email", column = @Column(name = "instructor_email", length = 128))
    })
    private Instructor instructor;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dayOfWeek", column = @Column(name = "schedule_day_of_week")),
            @AttributeOverride(name = "startTime", column = @Column(name = "schedule_start_time")),
            @AttributeOverride(name = "endTime", column = @Column(name = "schedule_end_time"))
    })
    private ScheduleSlot schedule;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer enrolled;

    public Course() {}

    public Course(String id, String code, String title, Instructor instructor,
                  ScheduleSlot schedule, Integer capacity, Integer enrolled) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.instructor = instructor;
        this.schedule = schedule;
        this.capacity = capacity;
        this.enrolled = enrolled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }
    public ScheduleSlot getSchedule() { return schedule; }
    public void setSchedule(ScheduleSlot schedule) { this.schedule = schedule; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }

    public boolean isFull() {
        return enrolled >= capacity;
    }

    public void enrollOne() {
        enrolled += 1;
    }

    public void withdrawOne() {
        enrolled -= 1;
    }
}
