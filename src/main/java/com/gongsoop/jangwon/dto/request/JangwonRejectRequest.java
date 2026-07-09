package com.gongsoop.jangwon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JangwonRejectRequest(

        @NotBlank(message = "관리자 메모를 입력해주세요")
        @Size(max = 1000, message = "관리자 메모는 1000자 이하로 입력해주세요")
        String adminMemo
) {
}