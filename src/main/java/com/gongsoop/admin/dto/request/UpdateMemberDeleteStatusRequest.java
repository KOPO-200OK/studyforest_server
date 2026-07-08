package com.gongsoop.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberDeleteStatusRequest(
        @NotNull(message = "삭제 상태를 입력해주세요")
        Boolean isDeleted
) {
}