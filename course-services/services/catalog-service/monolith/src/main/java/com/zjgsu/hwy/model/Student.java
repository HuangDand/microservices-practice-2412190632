package com.zjgsu.hwy.model;

public class Student {
    private String id;
    private String studentNumber;
    private String name;
    private String major;

    public Student(){}
    public Student(String id,String studentNumber,String name,String major){
        this.id=id;
        this.studentNumber=studentNumber;
        this.name=name;
        this.major=major;
    }

    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public String getStudentNumber(){return studentNumber;}
    public void setStudentNumber(String studentNumber){this.studentNumber=studentNumber;}
    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public String getMajor(){return major;}
    public void setMajor(String major){this.major=major;}
}
