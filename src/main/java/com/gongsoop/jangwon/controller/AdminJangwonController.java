package com.gongsoop.jangwon.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.jangwon.dto.request.JangwonRejectRequest;
import com.gongsoop.jangwon.dto.response.JangwonApplicationResponse;
import com.gongsoop.jangwon.service.JangwonService;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/jangwon")
public class AdminJangwonController {

    private final JangwonService jangwonService;

    public AdminJangwonController(JangwonService jangwonService) {
        this.jangwonService = jangwonService;
    }

    @GetMapping("/applications")
    public ApiResponse<PageResponse<JangwonApplicationResponse>> getApplications(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                "장원급제 신청 목록을 조회했습니다",
                jangwonService.getAdminApplications(email, page, size, status, keyword)
        );
    }

    @GetMapping("/applications/{jangwonApplicationId}")
    public ApiResponse<JangwonApplicationResponse> getApplicationDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long jangwonApplicationId
    ) {
        return ApiResponse.success(
                "장원급제 신청 상세를 조회했습니다",
                jangwonService.getAdminApplicationDetail(email, jangwonApplicationId)
        );
    }

    @PatchMapping("/applications/{jangwonApplicationId}/approve")
    public ApiResponse<JangwonApplicationResponse> approve(
            @AuthenticationPrincipal String email,
            @PathVariable Long jangwonApplicationId
    ) {
        return ApiResponse.success(
                "장원급제 신청을 승인했습니다",
                jangwonService.approve(email, jangwonApplicationId)
        );
    }

    @PatchMapping("/applications/{jangwonApplicationId}/reject")
    public ApiResponse<JangwonApplicationResponse> reject(
            @AuthenticationPrincipal String email,
            @PathVariable Long jangwonApplicationId,
            @Valid @RequestBody JangwonRejectRequest request
    ) {
        return ApiResponse.success(
                "장원급제 신청을 반려했습니다",
                jangwonService.reject(email, jangwonApplicationId, request)
        );
    }

    @DeleteMapping("/applications/{jangwonApplicationId}")
    public ApiResponse<Void> deleteApplication(
            @AuthenticationPrincipal String email,
            @PathVariable Long jangwonApplicationId
    ) {
        jangwonService.delete(email, jangwonApplicationId);

        return ApiResponse.success(
                "장원급제 신청을 삭제했습니다",
                null
        );
    }
}