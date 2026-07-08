package com.gongsoop.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminQuestionCreateRequest(
        @NotNull(message = "시험 회차를 입력해주세요")
        @Min(value = 1, message = "시험 회차는 1 이상이어야 합니다")
        Integer examRound,

        @NotNull(message = "문제 번호를 입력해주세요")
        @Min(value = 1, message = "문제 번호는 1 이상이어야 합니다")
        @Max(value = 100, message = "문제 번호는 100 이하여야 합니다")
        Integer qNo,

        @NotBlank(message = "문제 내용을 입력해주세요")
        @Size(max = 4000, message = "문제 내용은 4000자 이하로 입력해주세요")
        String questionText,

        @Size(max = 4000, message = "제시문은 4000자 이하로 입력해주세요")
        String passage,

        @NotNull(message = "배점을 입력해주세요")
        @Min(value = 1, message = "배점은 1점 이상이어야 합니다")
        @Max(value = 3, message = "배점은 3점 이하여야 합니다")
        Integer point,

        @NotBlank(message = "1번 보기를 입력해주세요")
        String choice1,

        @NotBlank(message = "2번 보기를 입력해주세요")
        String choice2,

        @NotBlank(message = "3번 보기를 입력해주세요")
        String choice3,

        @NotBlank(message = "4번 보기를 입력해주세요")
        String choice4,

        @NotBlank(message = "5번 보기를 입력해주세요")
        String choice5,

        @NotNull(message = "정답을 입력해주세요")
        @Min(value = 1, message = "정답은 1번 이상이어야 합니다")
        @Max(value = 5, message = "정답은 5번 이하여야 합니다")
        Integer answer,

        @Size(max = 100, message = "시대는 100자 이하로 입력해주세요")
        String era,

        @Size(max = 100, message = "분류는 100자 이하로 입력해주세요")
        String category
) {
}