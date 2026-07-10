package com.gongsoop.inquiry.dto.response;

import com.gongsoop.inquiry.entity.InquiryComment;

import java.time.LocalDateTime;

public record InquiryCommentResponse(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InquiryCommentResponse from(InquiryComment comment) {
        return new InquiryCommentResponse(
                comment.getCommentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
