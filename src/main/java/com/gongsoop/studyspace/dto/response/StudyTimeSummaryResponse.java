package com.gongsoop.studyspace.dto.response;

public record StudyTimeSummaryResponse(
        Long todayStudySeconds,
        Long weeklyStudySeconds
) {
}
