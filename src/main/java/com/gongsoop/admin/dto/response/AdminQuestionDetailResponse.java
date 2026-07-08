package com.gongsoop.admin.dto.response;

import java.util.List;

public record AdminQuestionDetailResponse(
        Long questionId,
        Integer examRound,
        Integer qNo,
        String questionText,
        String passage,
        Integer point,
        String era,
        String category,
        List<AdminQuestionChoiceResponse> choices,
        Integer answer,
        Boolean isDeleted
) {
}