package com.gongsoop.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "메시지를 입력해주세요")
        @Size(max = 1500, message = "메시지는 1500자 이하로 입력해주세요")
        String message
) {
}