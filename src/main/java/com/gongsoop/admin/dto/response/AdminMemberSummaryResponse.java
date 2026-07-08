package com.gongsoop.admin.dto.response;

import java.time.LocalDate;

public record AdminMemberSummaryResponse(
        Long memberId,
        String name,
        LocalDate birthdate,
        String email,
        String userRole,
        Boolean isDeleted
) {
}