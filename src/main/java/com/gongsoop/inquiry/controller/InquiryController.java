package com.gongsoop.inquiry.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.inquiry.dto.request.InquiryCreateRequest;
import com.gongsoop.inquiry.dto.request.InquiryUpdateRequest;
import com.gongsoop.inquiry.dto.response.InquiryDetailResponse;
import com.gongsoop.inquiry.dto.response.InquirySummaryResponse;
import com.gongsoop.inquiry.service.InquiryService;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/public")
    public ApiResponse<PageResponse<InquirySummaryResponse>> getPublicInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("공개 문의 목록을 조회했습니다", inquiryService.getPublicInquiries(page, size));
    }

    @GetMapping
    public ApiResponse<PageResponse<InquirySummaryResponse>> getMyInquiries(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("내 문의 목록을 조회했습니다", inquiryService.getMyInquiries(email, page, size));
    }

    @GetMapping("/{inquiryId}")
    public ApiResponse<InquiryDetailResponse> getMyInquiryDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId
    ) {
        return ApiResponse.success("문의 상세를 조회했습니다", inquiryService.getMyInquiryDetail(email, inquiryId));
    }

    @PostMapping
    public ApiResponse<InquiryDetailResponse> createInquiry(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody InquiryCreateRequest request
    ) {
        return ApiResponse.success("문의가 등록되었습니다", inquiryService.createInquiry(email, request));
    }

    @PutMapping("/{inquiryId}")
    public ApiResponse<InquiryDetailResponse> updateInquiry(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryUpdateRequest request
    ) {
        return ApiResponse.success("문의가 수정되었습니다", inquiryService.updateInquiry(email, inquiryId, request));
    }

    @DeleteMapping("/{inquiryId}")
    public ApiResponse<Void> deleteInquiry(
            @AuthenticationPrincipal String email,
            @PathVariable Long inquiryId
    ) {
        inquiryService.deleteInquiry(email, inquiryId);
        return ApiResponse.success("문의가 삭제되었습니다");
    }
}
