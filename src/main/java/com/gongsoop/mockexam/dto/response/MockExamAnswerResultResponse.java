package com.gongsoop.mockexam.dto.response;

public record MockExamAnswerResultResponse(
        Long questionId,
        Integer questionOrder,
        Integer selectedOptionId,
        Integer correctOptionId,
        Boolean isCorrect
) {
}