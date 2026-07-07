package com.gongsoop.question.dto.response;

public record SolveResultResponse(
        Boolean isCorrect,
        Integer correctOptionId,
        String explanation,
        Long solveRecordId
) {
}