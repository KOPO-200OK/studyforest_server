package com.gongsoop.dashboard.dto.response;

public record DashboardSummaryResponse(
        Long totalSolvedCount,
        Long correctCount,
        Long wrongCount,
        Double accuracyRate,

        Long histSolvedCount,
        Long aiGeneratedSolvedCount,

        Long unresolvedWrongCount,
        Long generatedQuestionSetCount,
        Long aiChatSessionCount,

        Long submittedMockExamCount,
        Double averageMockExamScore
) {
}