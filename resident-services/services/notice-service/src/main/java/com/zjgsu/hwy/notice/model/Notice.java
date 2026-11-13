package com.zjgsu.hwy.notice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notices")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 2000) private String content;
    @Column(nullable = false, length = 80) private String publisher;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected Notice() {}

    public Notice(String title, String content, String publisher) {
        this.title = title;
        this.content = content;
        this.publisher = publisher;
    }

    public void update(String title, String content, String publisher) {
        this.title = title;
        this.content = content;
        this.publisher = publisher;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getPublisher() { return publisher; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
