package com.gongsoop.dashboard.controller;

import com.gongsoop.dashboard.dto.response.DashboardResponse;
import com.gongsoop.dashboard.dto.response.DashboardSummaryResponse;
import com.gongsoop.dashboard.dto.response.RecentActivityResponse;
import com.gongsoop.dashboard.service.DashboardService;
import com.gongsoop.global.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "대시보드 정보를 조회했습니다",
                dashboardService.getDashboard(email)
        );
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "대시보드 요약 정보를 조회했습니다",
                dashboardService.getSummary(email)
        );
    }

    @GetMapping("/recent-activities")
    public ApiResponse<List<RecentActivityResponse>> getRecentActivities(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(
                "최근 학습 활동을 조회했습니다",
                dashboardService.getRecentActivities(email, limit)
        );
    }
}