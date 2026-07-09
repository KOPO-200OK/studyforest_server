package com.gongsoop.jangwon.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.jangwon.dto.request.JangwonApplyRequest;
import com.gongsoop.jangwon.dto.response.JangwonApplicationResponse;
import com.gongsoop.jangwon.dto.response.JangwonWinnerResponse;
import com.gongsoop.jangwon.service.JangwonService;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jangwon")
public class JangwonController {

    private final JangwonService jangwonService;

    public JangwonController(JangwonService jangwonService) {
        this.jangwonService = jangwonService;
    }

    @GetMapping
    public ApiResponse<PageResponse<JangwonWinnerResponse>> getApprovedWinners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "장원급제 목록을 조회했습니다",
                jangwonService.getApprovedWinners(page, size)
        );
    }

    @PostMapping("/applications")
    public ApiResponse<JangwonApplicationResponse> apply(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody JangwonApplyRequest request
    ) {
        return ApiResponse.success(
                "장원급제 인증을 신청했습니다",
                jangwonService.apply(email, request)
        );
    }

    @GetMapping("/applications/me")
    public ApiResponse<PageResponse<JangwonApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "내 장원급제 신청 내역을 조회했습니다",
                jangwonService.getMyApplications(email, page, size)
        );
    }
}