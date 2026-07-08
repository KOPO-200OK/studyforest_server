package com.gongsoop.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SolveAiGeneratedQuestionRequest(
        @Min(value = 1, message = "선택지는 1번 이상이어야 합니다")
        @Max(value = 10, message = "선택지는 10번 이하여야 합니다")
        Integer selectedChoiceIndex,

        @Size(max = 1000, message = "선택 답안은 1000자 이하로 입력해주세요")
        String selectedAnswerText
) {
}