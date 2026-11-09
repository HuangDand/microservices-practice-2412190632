package com.zjgsu.hwy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Instructor {

    @Column(name = "instructor_id", length = 64)
    private String id;

    @Column(name = "instructor_name", length = 64)
    private String name;

    @Column(name = "instructor_email", length = 128)
    private String email;

    public Instructor() {}

    public Instructor(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
