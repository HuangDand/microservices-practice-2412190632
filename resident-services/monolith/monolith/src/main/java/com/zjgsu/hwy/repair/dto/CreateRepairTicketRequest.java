package com.zjgsu.hwy.repair.dto;
import jakarta.validation.constraints.*;
public record CreateRepairTicketRequest(
 @NotBlank @Size(max=120) String title,
 @NotBlank @Size(max=2000) String description,
 @NotBlank @Size(max=120) String location,
 @NotNull Long residentId) {}
