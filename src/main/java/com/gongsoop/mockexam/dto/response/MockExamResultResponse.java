package com.gongsoop.mockexam.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MockExamResultResponse(
        Long mockExamId,
        String title,
        Integer totalQuestionCount,
        Integer correctCount,
        Double score,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt,
        List<MockExamAnswerResultResponse> answers
) {
}