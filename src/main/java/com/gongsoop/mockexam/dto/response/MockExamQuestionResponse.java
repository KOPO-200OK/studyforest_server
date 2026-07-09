package com.gongsoop.mockexam.dto.response;

import com.gongsoop.question.dto.response.QuestionDetailResponse;

public record MockExamQuestionResponse(
        Integer questionOrder,
        Integer selectedOptionId,
        QuestionDetailResponse question
) {
}