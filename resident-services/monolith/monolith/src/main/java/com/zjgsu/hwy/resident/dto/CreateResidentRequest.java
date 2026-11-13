package com.zjgsu.hwy.resident.dto;
import jakarta.validation.constraints.*;
public record CreateResidentRequest(
 @NotBlank @Size(max=80) String fullName,
 @NotBlank @Pattern(regexp="^[0-9+() -]{6,30}$",message="phone format is invalid") String phone,
 @NotBlank @Size(max=30) String building,
 @NotBlank @Size(max=30) String unitNumber) {}
