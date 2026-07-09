package com.gongsoop.jangwon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JangwonApplyRequest(

        @NotBlank(message = "표시할 닉네임을 입력해주세요")
        @Size(max = 100, message = "닉네임은 100자 이하로 입력해주세요")
        String displayNickname,

        @NotBlank(message = "캐릭터 이름을 입력해주세요")
        @Size(max = 100, message = "캐릭터 이름은 100자 이하로 입력해주세요")
        String characterName,

        @Size(max = 1000, message = "캐릭터 이미지 URL은 1000자 이하로 입력해주세요")
        String characterImageUrl,

        @NotBlank(message = "합격 인증 이미지를 입력해주세요")
        @Size(max = 5000000, message = "합격 인증 이미지는 5MB 이하로 입력해주세요")
        String certificateImageUrl
) {
}