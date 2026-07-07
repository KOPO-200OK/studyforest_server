package com.gongsoop.ai.dto.response;

import java.util.List;

public record GenerateAiQuestionResponse(
        List<GeneratedQuestionResponse> questions
) {
}