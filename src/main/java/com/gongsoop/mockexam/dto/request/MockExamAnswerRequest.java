package com.gongsoop.mockexam.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MockExamAnswerRequest(
        @NotNull(message = "문제 ID를 입력해주세요")
        Long questionId,

        @NotNull(message = "선택한 보기를 입력해주세요")
        @Min(value = 1, message = "보기 번호는 1번 이상이어야 합니다")
        @Max(value = 5, message = "보기 번호는 5번 이하여야 합니다")
        Integer selectedOptionId
) {
}