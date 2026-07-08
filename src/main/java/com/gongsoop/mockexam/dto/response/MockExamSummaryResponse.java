package com.gongsoop.mockexam.dto.response;

import java.time.LocalDateTime;

public record MockExamSummaryResponse(
        Long mockExamId,
        String title,
        Integer totalQuestionCount,
        Integer correctCount,
        Double score,
        String status,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
}