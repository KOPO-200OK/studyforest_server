package com.gongsoop.ai.dto.response;

import java.time.LocalDateTime;

public record ChatSessionSummaryResponse(
        Long aiChatSessionId,
        String title,
        Long questionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}