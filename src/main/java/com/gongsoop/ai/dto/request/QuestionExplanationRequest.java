package com.gongsoop.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record QuestionExplanationRequest(
        @Min(value = 1, message = "보기 번호는 1번 이상이어야 합니다")
        @Max(value = 5, message = "보기 번호는 5번 이하여야 합니다")
        Integer selectedOptionId
) {
}