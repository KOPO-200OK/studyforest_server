package com.gongsoop.notice.dto.response;

import com.gongsoop.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        Boolean isPinned,
        Boolean isPublished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.isPublished(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}