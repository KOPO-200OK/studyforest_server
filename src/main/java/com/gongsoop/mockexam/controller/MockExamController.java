package com.gongsoop.mockexam.controller;


import com.gongsoop.mockexam.dto.response.MockExamSummaryResponse;
import com.gongsoop.question.dto.response.PageResponse;
import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.mockexam.dto.request.MockExamCreateRequest;
import com.gongsoop.mockexam.dto.request.MockExamSubmitRequest;
import com.gongsoop.mockexam.dto.response.MockExamResultResponse;
import com.gongsoop.mockexam.dto.response.MockExamStartResponse;
import com.gongsoop.mockexam.service.MockExamService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mock-exams")
public class MockExamController {

    private final MockExamService mockExamService;

    public MockExamController(MockExamService mockExamService) {
        this.mockExamService = mockExamService;
    }

    @PostMapping
    public ApiResponse<MockExamStartResponse> createMockExam(
            @Valid @RequestBody MockExamCreateRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "모의고사를 생성했습니다",
                mockExamService.createMockExam(request, email)
        );
    }

    @GetMapping("/{mockExamId}")
    public ApiResponse<MockExamStartResponse> getMockExam(
            @PathVariable Long mockExamId,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "모의고사를 조회했습니다",
                mockExamService.getMockExam(mockExamId, email)
        );
    }

    @PostMapping("/{mockExamId}/submit")
    public ApiResponse<MockExamResultResponse> submitMockExam(
            @PathVariable Long mockExamId,
            @Valid @RequestBody MockExamSubmitRequest request,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "모의고사를 제출했습니다",
                mockExamService.submitMockExam(mockExamId, request, email)
        );
    }

    @GetMapping("/{mockExamId}/result")
    public ApiResponse<MockExamResultResponse> getMockExamResult(
            @PathVariable Long mockExamId,
            @AuthenticationPrincipal String email
    ) {
        return ApiResponse.success(
                "모의고사 결과를 조회했습니다",
                mockExamService.getMockExamResult(mockExamId, email)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<MockExamSummaryResponse>> getMockExamList(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "모의고사 목록을 조회했습니다",
                mockExamService.getMockExamList(email, page, size)
        );
    }
}