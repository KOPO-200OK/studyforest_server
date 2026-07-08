package com.gongsoop.study.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.study.dto.response.StudySummaryResponse;
import com.gongsoop.study.dto.response.WeaknessAnalysisResponse;
import com.gongsoop.study.service.StudyAnalysisService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/study")
public class StudyAnalysisController {

    private final StudyAnalysisService studyAnalysisService;

    public StudyAnalysisController(StudyAnalysisService studyAnalysisService) {
        this.studyAnalysisService = studyAnalysisService;
    }

    @GetMapping("/summary")
    public ApiResponse<StudySummaryResponse> getSummary(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "학습 요약 정보를 조회했습니다",
                studyAnalysisService.getSummary(email)
        );
    }

    @GetMapping("/weakness")
    public ApiResponse<WeaknessAnalysisResponse> getWeaknessAnalysis(
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "약점 분석 정보를 조회했습니다",
                studyAnalysisService.getWeaknessAnalysis(email)
        );
    }
}