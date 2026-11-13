package com.zjgsu.hwy.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String content,
        @NotBlank @Size(max = 80) String publisher) {}
