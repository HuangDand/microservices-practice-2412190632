package com.zjgsu.hwy.model;

public class Course {
    private String id;
    private String code;
    private String title;
    private Instructor instructor;
    private ScheduleSlot schedule;
    private Integer capacity;
    private Integer enrolled;

    //无参构造
    public Course(){}
    //全参构造
    public Course(String id, String code, String title, Instructor instructor, ScheduleSlot schedule, Integer capacity, Integer enrolled){
        this.id=id;
        this.code=code;
        this.title=title;
        this.instructor=instructor;
        this.schedule=schedule;
        this.capacity=capacity;
        this.enrolled=enrolled;
    }

    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public String getCode(){return code;}
    public void setCode(String code){this.code=code;}
    public String getTitle(){return title;}
    public void setTitle(String title){this.title=title;}
    public Instructor getInstructor(){return instructor;}
    public void setInstructor(Instructor instructor){this.instructor=instructor;}
    public ScheduleSlot getSchedule(){return schedule;}
    public void setSchedule(ScheduleSlot schedule){this.schedule=schedule;}
    public Integer getCapacity(){return capacity;}
    public void setCapacity(Integer capacity){this.capacity=capacity;}
    public Integer getEnrolled(){return enrolled;}
    public void setEnrolled(Integer enrolled){this.enrolled=enrolled;}

    public boolean isFull(){
        return enrolled >= capacity;
    }
    public void enrollOne(){
        enrolled +=1;
    }
    public void withdrawOne(){
        enrolled -=1;
    }
}
