package com.gongsoop.notice.dto.response;

import com.gongsoop.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeSummaryResponse(
        Long noticeId,
        String title,
        String contentPreview,
        Boolean isPinned,
        Boolean isPublished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                preview(notice.getContent(), 100),
                notice.isPinned(),
                notice.isPublished(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private static String preview(String content, int maxLength) {
        if (content == null) {
            return null;
        }

        String trimmed = content.trim();

        if (trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength) + "...";
    }
}