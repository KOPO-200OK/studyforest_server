package com.gongsoop.mockexam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MockExamSaveAnswersRequest(
        @NotEmpty(message = "저장할 답안을 입력해주세요")
        List<@Valid MockExamAnswerRequest> answers
) {
}