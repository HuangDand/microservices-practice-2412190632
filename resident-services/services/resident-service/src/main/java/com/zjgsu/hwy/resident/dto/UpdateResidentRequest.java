package com.zjgsu.hwy.resident.dto;
import jakarta.validation.constraints.*;
public record UpdateResidentRequest(
 @Size(max=80) String fullName,
 @Pattern(regexp="^[0-9+() -]{6,30}$",message="phone format is invalid") String phone,
 @Size(max=30) String building,
 @Size(max=30) String unitNumber) {}
