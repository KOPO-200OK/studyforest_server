package com.gongsoop.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChatSessionTitleRequest(
        @NotBlank(message = "채팅방 제목을 입력해주세요")
        @Size(max = 200, message = "채팅방 제목은 200자 이하로 입력해주세요")
        String title
) {
}