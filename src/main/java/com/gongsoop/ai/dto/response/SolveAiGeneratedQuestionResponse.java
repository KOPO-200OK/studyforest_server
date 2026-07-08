package com.gongsoop.ai.dto.response;

public record SolveAiGeneratedQuestionResponse(
        Long solveRecordId,
        Long aiGeneratedQuestionId,
        Integer selectedChoiceIndex,
        String selectedAnswerText,
        String correctAnswerText,
        Boolean isCorrect,
        String explanation
) {
}