package com.gongsoop.admin.dto.response;

public record AdminDashboardResponse(
        Long totalMemberCount,
        Long activeMemberCount,
        Long deletedMemberCount,
        Long histQuestionCount,
        Long histSolveRecordCount,
        Long mockExamCount,
        Long aiGeneratedQuestionSetCount,
        Long aiChatSessionCount
) {
}