package com.gongsoop.notice.controller;

import com.gongsoop.global.response.ApiResponse;
import com.gongsoop.notice.dto.response.NoticeDetailResponse;
import com.gongsoop.notice.dto.response.NoticeSummaryResponse;
import com.gongsoop.notice.service.NoticeService;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NoticeSummaryResponse>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "공지사항 목록을 조회했습니다",
                noticeService.getPublicNotices(page, size)
        );
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDetailResponse> getNoticeDetail(
            @PathVariable Long noticeId
    ) {
        return ApiResponse.success(
                "공지사항 상세를 조회했습니다",
                noticeService.getPublicNoticeDetail(noticeId)
        );
    }
}