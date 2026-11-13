package com.zjgsu.hwy.notice.repository;

import com.zjgsu.hwy.notice.model.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByCreatedAtDesc();

    List<Notice> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(String title, String content);
}
