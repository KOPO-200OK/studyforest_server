package com.gongsoop.mockexam.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MockExamStartResponse(
        Long mockExamId,
        String title,
        Integer totalQuestionCount,
        String status,
        LocalDateTime startedAt,
        List<MockExamQuestionResponse> questions
) {
}