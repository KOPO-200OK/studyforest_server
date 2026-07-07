package com.gongsoop.question.dto.response;

public record QuestionSummaryResponse(
        Long questionId,
        Integer examRound,
        Integer qNo,
        String era,
        String category,
        Integer point,
        String questionPreview
) {
}