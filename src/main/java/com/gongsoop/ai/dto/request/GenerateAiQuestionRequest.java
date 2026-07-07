package com.gongsoop.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GenerateAiQuestionRequest(
        @NotBlank(message = "주제를 입력해주세요")
        String topic,

        @Pattern(
                regexp = "basic|intermediate|advanced",
                message = "난이도는 basic, intermediate, advanced 중 하나여야 합니다"
        )
        String difficulty,

        @JsonProperty("question_type")
        @JsonAlias("questionType")
        @Pattern(
                regexp = "multiple_choice|short_answer|ox",
                message = "문제 유형이 올바르지 않습니다"
        )
        String questionType,

        @Min(value = 1, message = "문항 수는 1개 이상이어야 합니다")
        @Max(value = 10, message = "문항 수는 10개 이하여야 합니다")
        Integer count,

        @JsonProperty("include_explanation")
        @JsonAlias("includeExplanation")
        Boolean includeExplanation
) {

    public GenerateAiQuestionRequest {
        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "intermediate";
        }

        if (questionType == null || questionType.isBlank()) {
            questionType = "multiple_choice";
        }

        if (count == null) {
            count = 5;
        }

        if (includeExplanation == null) {
            includeExplanation = true;
        }
    }
}