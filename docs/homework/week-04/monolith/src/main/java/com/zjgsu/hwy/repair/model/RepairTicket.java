package com.zjgsu.hwy.repair.model;
import com.zjgsu.hwy.resident.model.Resident;
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
 @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="resident_id",nullable=false,foreignKey=@ForeignKey(name="fk_ticket_resident")) private Resident resident;
 @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(nullable=false) private Instant updatedAt=Instant.now();
 protected RepairTicket(){}
 public RepairTicket(String title,String description,String location,Resident resident){
  this.title=title;this.description=description;this.location=location;this.resident=resident;this.status=RepairStatus.OPEN;
 }
 public void changeStatus(RepairStatus value){this.status=value;this.updatedAt=Instant.now();}
 public Long getId(){return id;} public String getTitle(){return title;}
 public String getDescription(){return description;} public String getLocation(){return location;}
 public RepairStatus getStatus(){return status;} public Resident getResident(){return resident;}
 public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
