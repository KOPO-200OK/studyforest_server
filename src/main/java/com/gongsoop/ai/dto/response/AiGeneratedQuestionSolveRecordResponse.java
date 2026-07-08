package com.gongsoop.ai.dto.response;

import java.time.LocalDateTime;

public record AiGeneratedQuestionSolveRecordResponse(
        Long solveRecordId,
        Long aiGeneratedQuestionId,
        String question,
        Integer selectedChoiceIndex,
        String selectedAnswerText,
        String correctAnswerText,
        Boolean isCorrect,
        LocalDateTime solvedAt
) {
}