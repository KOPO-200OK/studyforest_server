package com.gongsoop.inquiry.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.inquiry.dto.request.InquiryCommentCreateRequest;
import com.gongsoop.inquiry.dto.request.InquiryCommentUpdateRequest;
import com.gongsoop.inquiry.dto.response.InquiryCommentResponse;
import com.gongsoop.inquiry.dto.response.InquiryDetailResponse;
import com.gongsoop.inquiry.dto.response.InquirySummaryResponse;
import com.gongsoop.inquiry.service.InquiryService;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InquirySummaryResponse>> getAdminInquiries(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success("문의 목록을 조회했습니다", inquiryService.getAdminInquiries(email, page, size, keyword));
    }

    @GetMapping("/{inquiryId}")
    public ApiResponse<InquiryDetailResponse> getAdminInquiryDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId
    ) {
        return ApiResponse.success("문의 상세를 조회했습니다", inquiryService.getAdminInquiryDetail(email, inquiryId));
    }

    @PostMapping("/{inquiryId}/comments")
    public ApiResponse<InquiryCommentResponse> createComment(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryCommentCreateRequest request
    ) {
        return ApiResponse.success("답변이 등록되었습니다", inquiryService.createComment(email, inquiryId, request));
    }

    @PutMapping("/{inquiryId}/comments/{commentId}")
    public ApiResponse<InquiryCommentResponse> updateComment(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId,
            @Valid @RequestBody InquiryCommentUpdateRequest request
    ) {
        return ApiResponse.success("답변이 수정되었습니다", inquiryService.updateComment(email, inquiryId, commentId, request));
    }

    @DeleteMapping("/{inquiryId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId
    ) {
        inquiryService.deleteComment(email, inquiryId, commentId);
        return ApiResponse.success("답변이 삭제되었습니다");
    }
}
