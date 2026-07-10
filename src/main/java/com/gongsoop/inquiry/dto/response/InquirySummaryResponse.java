package com.gongsoop.inquiry.dto.response;

import com.gongsoop.inquiry.entity.Inquiry;

import java.time.LocalDateTime;

public record InquirySummaryResponse(
        Long inquiryId,
        String title,
        Boolean isSecret,
        Boolean hasComment,
        String authorName,
        LocalDateTime createdAt
) {

    public static InquirySummaryResponse from(Inquiry inquiry, boolean hasComment) {
        return new InquirySummaryResponse(
                inquiry.getInquiryId(),
                inquiry.getTitle(),
                inquiry.isSecret(),
                hasComment,
                inquiry.getMember().getName(),
                inquiry.getCreatedAt()
        );
    }
}
