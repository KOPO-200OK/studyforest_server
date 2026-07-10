package com.gongsoop.inquiry.dto.response;

import com.gongsoop.inquiry.entity.Inquiry;
import com.gongsoop.inquiry.entity.InquiryComment;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryDetailResponse(
        Long inquiryId,
        String title,
        String content,
        Boolean isSecret,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InquiryCommentResponse> comments
) {

    public static InquiryDetailResponse from(Inquiry inquiry, List<InquiryComment> comments) {
        return new InquiryDetailResponse(
                inquiry.getInquiryId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.isSecret(),
                inquiry.getMember().getName(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt(),
                comments.stream().map(InquiryCommentResponse::from).toList()
        );
    }
}
