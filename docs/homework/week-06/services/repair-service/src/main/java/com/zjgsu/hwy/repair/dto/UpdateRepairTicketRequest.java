package com.zjgsu.hwy.repair.dto;
import jakarta.validation.constraints.*;
public record UpdateRepairTicketRequest(
 @Size(max=120) String title,
 @Size(max=2000) String description,
 @Size(max=120) String location) {}
