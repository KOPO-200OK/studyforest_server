package com.gongsoop.notice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.notice.dto.request.NoticeCreateRequest;
import com.gongsoop.notice.dto.request.NoticeUpdateRequest;
import com.gongsoop.notice.dto.response.NoticeDetailResponse;
import com.gongsoop.notice.dto.response.NoticeSummaryResponse;
import com.gongsoop.notice.service.NoticeService;
import com.gongsoop.question.dto.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    public AdminNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    public ApiResponse<PageResponse<NoticeSummaryResponse>> getAdminNotices(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublished
    ) {
        return ApiResponse.success(
                "관리자 공지사항 목록을 조회했습니다",
                noticeService.getAdminNotices(email, page, size, keyword, isPublished)
        );
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDetailResponse> getAdminNoticeDetail(
            @AuthenticationPrincipal String email,
            @PathVariable Long noticeId
    ) {
        return ApiResponse.success(
                "관리자 공지사항 상세를 조회했습니다",
                noticeService.getAdminNoticeDetail(email, noticeId)
        );
    }

    @PostMapping
    public ApiResponse<NoticeDetailResponse> createNotice(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        return ApiResponse.success(
                "공지사항을 생성했습니다",
                noticeService.createNotice(email, request)
        );
    }

    @PutMapping("/{noticeId}")
    public ApiResponse<NoticeDetailResponse> updateNotice(
            @AuthenticationPrincipal String email,
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return ApiResponse.success(
                "공지사항을 수정했습니다",
                noticeService.updateNotice(email, noticeId, request)
        );
    }

    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @AuthenticationPrincipal String email,
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(email, noticeId);

        return ApiResponse.success("공지사항을 삭제했습니다");
    }
}