package com.gongsoop.member.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCharacterRequest(
        @NotNull(message = "캐릭터를 선택해주세요")
        @Min(value = 1, message = "유효하지 않은 캐릭터입니다")
        @Max(value = 8, message = "유효하지 않은 캐릭터입니다")
        Integer characterId
) {
}
