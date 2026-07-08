package com.gongsoop.question.dto.response;

import java.time.LocalDateTime;

public record WrongAnswerSummaryResponse(
        Long wrongAnswerId,
        QuestionSummaryResponse question,
        Integer wrongCount,
        Boolean isResolved,
        Integer lastSelectedAnswer,
        Integer correctAnswer,
        LocalDateTime createdAt
) {
}