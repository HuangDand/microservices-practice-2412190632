package com.zjgsu.hwy.repair.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="repair_tickets", indexes={
 @Index(name="idx_ticket_status", columnList="status"),
 @Index(name="idx_ticket_resident", columnList="resident_id")})
public class RepairTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=2000) private String description;
 @Column(nullable=false,length=120) private String location;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private RepairStatus status=RepairStatus.OPEN;
 @Column(name="resident_id",nullable=false) private Long residentId;
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(nullable=false) private Instant updatedAt=Instant.now();
 protected RepairTicket(){}
 public RepairTicket(String title,String description,String location,Long residentId){
  this.title=title;this.description=description;this.location=location;this.residentId=residentId;this.status=RepairStatus.OPEN;
 }
 public void changeStatus(RepairStatus value){this.status=value;this.updatedAt=Instant.now();}
 public void update(String title,String description,String location){
  this.title=title;this.description=description;this.location=location;this.updatedAt=Instant.now();
 }
 public Long getId(){return id;} public String getTitle(){return title;}
 public String getDescription(){return description;} public String getLocation(){return location;}
 public RepairStatus getStatus(){return status;} public Long getResidentId(){return residentId;}
 public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
