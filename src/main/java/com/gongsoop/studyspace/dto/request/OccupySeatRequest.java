package com.gongsoop.studyspace.dto.request;

import jakarta.validation.constraints.Size;

public record OccupySeatRequest(
        @Size(max = 50, message = "학습 과목은 50자 이하여야 합니다")
        String subject
) {
}
