package com.zjgsu.hwy.notice.dto;

import jakarta.validation.constraints.Size;

public record UpdateNoticeRequest(
        @Size(max = 120) String title,
        @Size(max = 2000) String content,
        @Size(max = 80) String publisher) {}
