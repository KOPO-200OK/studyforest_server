package com.gongsoop.admin.studyroom.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminSeatActiveRequest(
        @NotNull(message = "좌석 활성화 여부를 입력해주세요")
        Boolean active
) {
}