package com.gongsoop.member.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(
                message = "닉네임을 입력해주세요"
        )
        @Size(
                max = 16,
                message = "닉네임은 16자 이하로 입력해주세요"
        )
        String nickname,

        @NotNull(
                message = "캐릭터를 선택해주세요"
        )
        @Min(
                value = 1,
                message = "유효하지 않은 캐릭터입니다"
        )
        @Max(
                value = 8,
                message = "유효하지 않은 캐릭터입니다"
        )
        Integer characterId
) {
}