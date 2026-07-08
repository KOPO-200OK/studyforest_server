package com.gongsoop.ai.dto.response;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long aiChatMessageId,
        String sender,
        String message,
        LocalDateTime createdAt
) {
}