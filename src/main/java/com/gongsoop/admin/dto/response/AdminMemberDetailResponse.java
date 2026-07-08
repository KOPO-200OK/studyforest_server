package com.gongsoop.admin.dto.response;

import java.time.LocalDate;

public record AdminMemberDetailResponse(
        Long memberId,
        String name,
        LocalDate birthdate,
        String email,
        String userRole,
        Boolean isDeleted,
        Long histSolvedCount,
        Long aiGeneratedSolvedCount,
        Long mockExamCount,
        Long aiGeneratedQuestionSetCount,
        Long aiChatSessionCount
) {
}