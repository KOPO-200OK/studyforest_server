package com.gongsoop.dashboard.dto.response;

import java.time.LocalDateTime;

public record RecentActivityResponse(
        String activityType,
        String title,
        String description,
        Long targetId,
        LocalDateTime createdAt
) {
}