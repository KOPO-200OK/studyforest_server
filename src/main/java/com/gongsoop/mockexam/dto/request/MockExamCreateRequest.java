package com.gongsoop.mockexam.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MockExamCreateRequest(
        @Min(value = 1, message = "문제 수는 1개 이상이어야 합니다")
        @Max(value = 50, message = "문제 수는 50개 이하여야 합니다")
        Integer count,

        Integer examRound,

        String era,

        String category,

        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
        String title
) {
}