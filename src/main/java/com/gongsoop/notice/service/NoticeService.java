package com.gongsoop.notice.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.notice.dto.request.NoticeCreateRequest;
import com.gongsoop.notice.dto.request.NoticeUpdateRequest;
import com.gongsoop.notice.dto.response.NoticeDetailResponse;
import com.gongsoop.notice.dto.response.NoticeSummaryResponse;
import com.gongsoop.notice.entity.Notice;
import com.gongsoop.notice.repository.NoticeRepository;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public PageResponse<NoticeSummaryResponse> getPublicNotices(int page, int size) {
        PageRequest pageRequest = createPageRequest(page, size);

        Page<Notice> noticePage = noticeRepository.findByIsPublished(1, pageRequest);

        return toSummaryPageResponse(noticePage);
    }

    public NoticeDetailResponse getPublicNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findByNoticeIdAndIsPublished(noticeId, 1)
                .orElseThrow(() -> new BusinessException(
                        "NOTICE_NOT_FOUND",
                        "공지사항을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        return NoticeDetailResponse.from(notice);
    }

    public PageResponse<NoticeSummaryResponse> getAdminNotices(
            int page,
            int size,
            String keyword,
            Boolean isPublished
    ) {
        PageRequest pageRequest = createPageRequest(page, size);

        String searchKeyword = hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        Integer publishedValue = isPublished == null ? null : Boolean.TRUE.equals(isPublished) ? 1 : 0;

        Page<Notice> noticePage = noticeRepository.searchForAdmin(
                searchKeyword,
                publishedValue,
                pageRequest
        );

        return toSummaryPageResponse(noticePage);
    }

    public NoticeDetailResponse getAdminNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(
                        "NOTICE_NOT_FOUND",
                        "공지사항을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        return NoticeDetailResponse.from(notice);
    }

    @Transactional
    public NoticeDetailResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.create(
                request.title().trim(),
                request.content().trim(),
                Boolean.TRUE.equals(request.isPinned()),
                request.isPublished() == null || Boolean.TRUE.equals(request.isPublished())
        );

        Notice savedNotice = noticeRepository.save(notice);

        return NoticeDetailResponse.from(savedNotice);
    }

    @Transactional
    public NoticeDetailResponse updateNotice(
            Long noticeId,
            NoticeUpdateRequest request
    ) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(
                        "NOTICE_NOT_FOUND",
                        "공지사항을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        notice.update(
                request.title().trim(),
                request.content().trim(),
                Boolean.TRUE.equals(request.isPinned()),
                Boolean.TRUE.equals(request.isPublished())
        );

        return NoticeDetailResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(
                        "NOTICE_NOT_FOUND",
                        "공지사항을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        noticeRepository.delete(notice);
    }

    private PageRequest createPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("isPinned"),
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("noticeId")
                )
        );
    }

    private PageResponse<NoticeSummaryResponse> toSummaryPageResponse(Page<Notice> noticePage) {
        return new PageResponse<>(
                noticePage.getContent()
                        .stream()
                        .map(NoticeSummaryResponse::from)
                        .toList(),
                noticePage.getTotalElements(),
                noticePage.getTotalPages(),
                noticePage.getNumber(),
                noticePage.getSize()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}