package com.zjgsu.hwy.resident.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="residents", uniqueConstraints=@UniqueConstraint(name="uk_resident_phone", columnNames="phone"))
public class Resident {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=80) private String fullName;
    @Column(nullable=false, length=30, unique=true) private String phone;
    @Column(nullable=false, length=30) private String building;
    @Column(nullable=false, length=30) private String unitNumber;
    @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
    protected Resident() {}
    public Resident(String fullName, String phone, String building, String unitNumber) {
        this.fullName=fullName; this.phone=phone; this.building=building; this.unitNumber=unitNumber;
    }
    public void update(String fullName, String phone, String building, String unitNumber) {
        this.fullName=fullName; this.phone=phone; this.building=building; this.unitNumber=unitNumber;
    }
    public Long getId(){return id;} public String getFullName(){return fullName;}
    public String getPhone(){return phone;} public String getBuilding(){return building;}
    public String getUnitNumber(){return unitNumber;} public Instant getCreatedAt(){return createdAt;}
}
