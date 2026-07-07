package com.gongsoop.question.dto.response;

public record QuestionOptionResponse(
        Integer questionOptionId,
        Integer optionNo,
        String optionContent,
        Boolean isCorrect,
        String optionExplanation
) {
}