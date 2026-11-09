package com.zjgsu.hwy.model;
import java.time.LocalDateTime;

public class Enrollment {
    private String id;
    private String studentId;
    private String courseId;
    private LocalDateTime enrolledAt;
    private EnrollmentStatus status;

    public Enrollment(){}
    public Enrollment(String id,String studentId,String courseId,LocalDateTime enrolledAt,EnrollmentStatus status){
        this.id=id;
        this.studentId=studentId;
        this.courseId=courseId;
        this.enrolledAt=enrolledAt;
        this.status=status;
    }

    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public String getStudentId(){return studentId;}
    public void setStudentId(String studentId){this.studentId=studentId;}
    public String getCourseId(){return courseId;}
    public void setCourseId(String courseId){this.courseId=courseId;}
    public LocalDateTime getEnrolledAt(){return enrolledAt;}
    public void setEnrolledAt(LocalDateTime enrolledAt){this.enrolledAt=enrolledAt;}
    public EnrollmentStatus getStatus(){return status;}
    public void setStatus(EnrollmentStatus status){this.status=status;}
}
