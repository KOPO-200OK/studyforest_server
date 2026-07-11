package com.gongsoop.study.dto.response;

public record StudySummaryResponse(
        Long totalSolvedCount,
        Long correctCount,
        Long wrongCount,
        Double accuracyRate,
        Long unresolvedWrongCount,
        Long submittedMockExamCount,
        Double averageMockExamScore,
        Long todayStudySeconds,
        Long weeklyStudySeconds
) {
}