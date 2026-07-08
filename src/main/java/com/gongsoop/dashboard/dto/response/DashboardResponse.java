package com.gongsoop.dashboard.dto.response;

import java.util.List;

public record DashboardResponse(
        DashboardSummaryResponse summary,
        List<RecentActivityResponse> recentActivities
) {
}