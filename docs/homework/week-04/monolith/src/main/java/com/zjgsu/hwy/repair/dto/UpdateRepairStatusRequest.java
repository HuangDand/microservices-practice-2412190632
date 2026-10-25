package com.zjgsu.hwy.repair.dto;
import com.zjgsu.hwy.repair.model.RepairStatus;
import jakarta.validation.constraints.NotNull;
public record UpdateRepairStatusRequest(@NotNull RepairStatus status) {}
