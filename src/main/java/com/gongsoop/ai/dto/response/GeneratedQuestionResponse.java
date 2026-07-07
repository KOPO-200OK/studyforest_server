package com.gongsoop.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record GeneratedQuestionResponse(
        String question,
        List<String> choices,
        String answer,
        String explanation,
        String era,
        String topic,
        String difficulty,

        @JsonAlias("exam_tip")
        String examTip
) {
}