package com.gongsoop.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AiGeneratedQuestionSetDetailResponse(
        Long aiGeneratedQuestionSetId,
        String topic,
        String difficulty,
        String questionType,
        Integer questionCount,
        Boolean includeExplanation,
        LocalDateTime createdAt,
        List<AiGeneratedQuestionDetailResponse> questions
) {
}