package com.gongsoop.ai.dto.response;

import java.util.List;

public record AiGeneratedQuestionDetailResponse(
        Long aiGeneratedQuestionId,
        Integer questionOrder,
        String question,
        List<String> choices,
        String answer,
        String explanation,
        String era,
        String topic,
        String difficulty,
        String examTip
) {
}