package com.gongsoop.ai.dto.response;

import java.time.LocalDateTime;

public record AiGeneratedQuestionSetSummaryResponse(
        Long aiGeneratedQuestionSetId,
        String topic,
        String difficulty,
        String questionType,
        Integer questionCount,
        Boolean includeExplanation,
        LocalDateTime createdAt
) {
}