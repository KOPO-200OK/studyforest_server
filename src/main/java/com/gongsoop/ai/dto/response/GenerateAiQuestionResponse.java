package com.gongsoop.ai.dto.response;

import java.util.List;

public record GenerateAiQuestionResponse(
        Long aiGeneratedQuestionSetId,
        List<GeneratedQuestionResponse> questions
) {
}