package com.zjgsu.hwy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
public class ScheduleSlot {

    @Column(name = "schedule_day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "schedule_start_time")
    private LocalTime startTime;

    @Column(name = "schedule_end_time")
    private LocalTime endTime;

    public ScheduleSlot() {}

    public ScheduleSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
