package com.gongsoop.admin.dto.response;

import java.time.LocalDate;

public record AdminMemberSummaryResponse(
        Long memberId,
        String name,
        String nickname,
        LocalDate birthdate,
        String email,
        String userRole,
        Integer characterId,
        Boolean isDeleted
) {
}