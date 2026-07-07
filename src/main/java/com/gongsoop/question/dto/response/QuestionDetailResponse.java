package com.gongsoop.question.dto.response;

import java.util.List;

public record QuestionDetailResponse(
        Long questionId,
        Integer examRound,
        Integer qNo,
        String questionContent,
        String passage,
        Integer point,
        String era,
        String category,
        List<QuestionOptionResponse> options
) {
}