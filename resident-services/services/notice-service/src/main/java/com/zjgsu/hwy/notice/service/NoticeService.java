package com.zjgsu.hwy.notice.service;

import com.zjgsu.hwy.common.ResourceNotFoundException;
import com.zjgsu.hwy.notice.dto.CreateNoticeRequest;
import com.zjgsu.hwy.notice.dto.UpdateNoticeRequest;
import com.zjgsu.hwy.notice.model.Notice;
import com.zjgsu.hwy.notice.repository.NoticeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {
    private final NoticeRepository notices;

    public NoticeService(NoticeRepository notices) {
        this.notices = notices;
    }

    @Transactional
    public Notice create(CreateNoticeRequest r) {
        return notices.save(new Notice(r.title().trim(), r.content().trim(), r.publisher().trim()));
    }

    @Transactional(readOnly = true)
    public List<Notice> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return notices.findAllByOrderByCreatedAtDesc();
        }
        String value = keyword.trim();
        return notices.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(value, value);
    }

    @Transactional(readOnly = true)
    public Notice get(Long id) {
        return notices.findById(id).orElseThrow(() -> new ResourceNotFoundException("公告不存在: " + id));
    }

    @Transactional
    public Notice update(Long id, UpdateNoticeRequest r) {
        Notice notice = get(id);
        notice.update(
                r.title() != null ? r.title().trim() : notice.getTitle(),
                r.content() != null ? r.content().trim() : notice.getContent(),
                r.publisher() != null ? r.publisher().trim() : notice.getPublisher());
        return notices.save(notice);
    }

    @Transactional
    public void delete(Long id) {
        notices.delete(get(id));
    }
}
