package com.gongsoop.admin.dto.response;

public record AdminQuestionSummaryResponse(
        Long questionId,
        Integer examRound,
        Integer qNo,
        String era,
        String category,
        Integer point,
        String questionPreview,
        Boolean isDeleted
) {
}