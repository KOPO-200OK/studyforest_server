package com.gongsoop.inquiry.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.inquiry.dto.request.InquiryCommentCreateRequest;
import com.gongsoop.inquiry.dto.request.InquiryCommentUpdateRequest;
import com.gongsoop.inquiry.dto.request.InquiryCreateRequest;
import com.gongsoop.inquiry.dto.request.InquiryUpdateRequest;
import com.gongsoop.inquiry.dto.response.InquiryCommentResponse;
import com.gongsoop.inquiry.dto.response.InquiryDetailResponse;
import com.gongsoop.inquiry.dto.response.InquirySummaryResponse;
import com.gongsoop.inquiry.entity.Inquiry;
import com.gongsoop.inquiry.entity.InquiryComment;
import com.gongsoop.inquiry.repository.InquiryCommentRepository;
import com.gongsoop.inquiry.repository.InquiryRepository;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.entity.MemberRole;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.question.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final MemberRepository memberRepository;

    public InquiryService(
            InquiryRepository inquiryRepository,
            InquiryCommentRepository inquiryCommentRepository,
            MemberRepository memberRepository
    ) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryCommentRepository = inquiryCommentRepository;
        this.memberRepository = memberRepository;
    }

    // 공개: 비밀글 제외 전체 문의 목록
    public PageResponse<InquirySummaryResponse> getPublicInquiries(int page, int size) {
        Page<Inquiry> inquiryPage = inquiryRepository.findByIsSecretFalse(createPageRequest(page, size));
        return toSummaryPageResponse(inquiryPage);
    }

    // 사용자: 내 문의 목록
    public PageResponse<InquirySummaryResponse> getMyInquiries(String email, int page, int size) {
        Member member = findActiveMember(email);
        PageRequest pageRequest = createPageRequest(page, size);

        Page<Inquiry> inquiryPage = inquiryRepository.findByMember(member, pageRequest);

        return toSummaryPageResponse(inquiryPage);
    }

    // 사용자: 내 문의 상세
    public InquiryDetailResponse getMyInquiryDetail(String email, Long inquiryId) {
        Member member = findActiveMember(email);
        Inquiry inquiry = findInquiry(inquiryId);

        if (!inquiry.getMember().getId().equals(member.getId())) {
            throw new BusinessException("INQUIRY_FORBIDDEN", "본인의 문의만 조회할 수 있습니다", HttpStatus.FORBIDDEN);
        }

        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
        return InquiryDetailResponse.from(inquiry, comments);
    }

    // 사용자: 문의 작성
    @Transactional
    public InquiryDetailResponse createInquiry(String email, InquiryCreateRequest request) {
        Member member = findActiveMember(email);

        Inquiry inquiry = Inquiry.create(
                member,
                request.title().trim(),
                request.content().trim(),
                Boolean.TRUE.equals(request.isSecret())
        );

        Inquiry saved = inquiryRepository.save(inquiry);
        return InquiryDetailResponse.from(saved, List.of());
    }

    // 사용자: 문의 수정 (댓글이 없을 때만)
    @Transactional
    public InquiryDetailResponse updateInquiry(String email, Long inquiryId, InquiryUpdateRequest request) {
        Member member = findActiveMember(email);
        Inquiry inquiry = findInquiry(inquiryId);

        if (!inquiry.getMember().getId().equals(member.getId())) {
            throw new BusinessException("INQUIRY_FORBIDDEN", "본인의 문의만 수정할 수 있습니다", HttpStatus.FORBIDDEN);
        }

        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
        if (!comments.isEmpty()) {
            throw new BusinessException("INQUIRY_ALREADY_ANSWERED", "답변이 달린 문의는 수정할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        inquiry.update(request.title().trim(), request.content().trim(), Boolean.TRUE.equals(request.isSecret()));
        return InquiryDetailResponse.from(inquiry, List.of());
    }

    // 사용자: 문의 삭제 (댓글이 없을 때만)
    @Transactional
    public void deleteInquiry(String email, Long inquiryId) {
        Member member = findActiveMember(email);
        Inquiry inquiry = findInquiry(inquiryId);

        if (!inquiry.getMember().getId().equals(member.getId())) {
            throw new BusinessException("INQUIRY_FORBIDDEN", "본인의 문의만 삭제할 수 있습니다", HttpStatus.FORBIDDEN);
        }

        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
        if (!comments.isEmpty()) {
            throw new BusinessException("INQUIRY_ALREADY_ANSWERED", "답변이 달린 문의는 삭제할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        inquiryRepository.delete(inquiry);
    }

    // 관리자: 전체 문의 목록
    public PageResponse<InquirySummaryResponse> getAdminInquiries(String email, int page, int size, String keyword) {
        validateAdmin(email);
        PageRequest pageRequest = createPageRequest(page, size);

        String searchKeyword = hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        Page<Inquiry> inquiryPage = inquiryRepository.searchForAdmin(searchKeyword, pageRequest);

        return toSummaryPageResponse(inquiryPage);
    }

    // 관리자: 문의 상세
    public InquiryDetailResponse getAdminInquiryDetail(String email, Long inquiryId) {
        validateAdmin(email);
        Inquiry inquiry = findInquiry(inquiryId);

        List<InquiryComment> comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(inquiry);
        return InquiryDetailResponse.from(inquiry, comments);
    }

    // 관리자: 댓글 작성
    @Transactional
    public InquiryCommentResponse createComment(String email, Long inquiryId, InquiryCommentCreateRequest request) {
        validateAdmin(email);
        Inquiry inquiry = findInquiry(inquiryId);

        InquiryComment comment = InquiryComment.create(inquiry, request.content().trim());
        InquiryComment saved = inquiryCommentRepository.save(comment);

        return InquiryCommentResponse.from(saved);
    }

    // 관리자: 댓글 수정
    @Transactional
    public InquiryCommentResponse updateComment(String email, Long inquiryId, Long commentId, InquiryCommentUpdateRequest request) {
        validateAdmin(email);
        Inquiry inquiry = findInquiry(inquiryId);

        InquiryComment comment = inquiryCommentRepository.findByCommentIdAndInquiry(commentId, inquiry)
                .orElseThrow(() -> new BusinessException("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        comment.update(request.content().trim());
        return InquiryCommentResponse.from(comment);
    }

    // 관리자: 댓글 삭제
    @Transactional
    public void deleteComment(String email, Long inquiryId, Long commentId) {
        validateAdmin(email);
        Inquiry inquiry = findInquiry(inquiryId);

        InquiryComment comment = inquiryCommentRepository.findByCommentIdAndInquiry(commentId, inquiry)
                .orElseThrow(() -> new BusinessException("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        inquiryCommentRepository.delete(comment);
    }

    private Inquiry findInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException("INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }

    private Member findActiveMember(String email) {
        if (!hasText(email)) {
            throw new BusinessException("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
        }
        return memberRepository.findByEmail(email)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }

    private void validateAdmin(String email) {
        Member member = findActiveMember(email);
        if (member.getUserRole() != MemberRole.ADMIN) {
            throw new BusinessException("ADMIN_FORBIDDEN", "관리자만 접근할 수 있습니다", HttpStatus.FORBIDDEN);
        }
    }

    private PageRequest createPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("inquiryId")));
    }

    private PageResponse<InquirySummaryResponse> toSummaryPageResponse(Page<Inquiry> page) {
        return new PageResponse<>(
                page.getContent().stream()
                        .map(i -> {
                            List<InquiryComment> comments = inquiryCommentRepository.findByInquiryOrderByCreatedAtAsc(i);
                            return InquirySummaryResponse.from(i, !comments.isEmpty());
                        })
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
