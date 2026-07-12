package com.gongsoop.admin.dto.response;

import java.time.LocalDate;

public record AdminMemberDetailResponse(
        Long memberId,
        String name,
        String nickname,
        LocalDate birthdate,
        String email,
        String userRole,
        Integer characterId,
        Boolean isDeleted,
        Long histSolvedCount,
        Long aiGeneratedSolvedCount,
        Long mockExamCount,
        Long aiGeneratedQuestionSetCount,
        Long aiChatSessionCount
) {
}