package com.gongsoop.jangwon.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.jangwon.dto.request.JangwonApplyRequest;
import com.gongsoop.jangwon.dto.request.JangwonRejectRequest;
import com.gongsoop.jangwon.dto.response.JangwonApplicationResponse;
import com.gongsoop.jangwon.dto.response.JangwonWinnerResponse;
import com.gongsoop.jangwon.entity.JangwonApplication;
import com.gongsoop.jangwon.entity.JangwonStatus;
import com.gongsoop.jangwon.repository.JangwonApplicationRepository;
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

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class JangwonService {

    private final JangwonApplicationRepository jangwonApplicationRepository;
    private final MemberRepository memberRepository;

    public JangwonService(
            JangwonApplicationRepository jangwonApplicationRepository,
            MemberRepository memberRepository
    ) {
        this.jangwonApplicationRepository = jangwonApplicationRepository;
        this.memberRepository = memberRepository;
    }

    public PageResponse<JangwonWinnerResponse> getApprovedWinners(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(
                        Sort.Order.desc("reviewedAt"),
                        Sort.Order.desc("jangwonApplicationId")
                )
        );

        Page<JangwonApplication> winnerPage =
                jangwonApplicationRepository.findByStatusAndIsVisible(
                        JangwonStatus.APPROVED,
                        1,
                        pageRequest
                );

        return new PageResponse<>(
                winnerPage.getContent()
                        .stream()
                        .map(JangwonWinnerResponse::from)
                        .toList(),
                winnerPage.getTotalElements(),
                winnerPage.getTotalPages(),
                winnerPage.getNumber(),
                winnerPage.getSize()
        );
    }

    @Transactional
    public JangwonApplicationResponse apply(String email, JangwonApplyRequest request) {
        Member member = getCurrentMember(email);

        if (jangwonApplicationRepository.existsByMemberIdAndStatus(member.getId(), JangwonStatus.PENDING)) {
            throw new BusinessException(
                    "JANGWON_PENDING_EXISTS",
                    "이미 심사 중인 장원급제 신청이 있습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        JangwonApplication application = JangwonApplication.create(
                member.getId(),
                request.displayNickname().trim(),
                member.getName(),
                request.characterName().trim(),
                trimToNull(request.characterImageUrl()),
                request.certificateImageUrl().trim()
        );

        JangwonApplication savedApplication = jangwonApplicationRepository.save(application);

        return JangwonApplicationResponse.from(savedApplication);
    }

    public PageResponse<JangwonApplicationResponse> getMyApplications(
            String email,
            int page,
            int size
    ) {
        Member member = getCurrentMember(email);

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        Page<JangwonApplication> applicationPage =
                jangwonApplicationRepository.findByMemberIdOrderByAppliedAtDesc(
                        member.getId(),
                        pageRequest
                );

        return toApplicationPageResponse(applicationPage);
    }

    public PageResponse<JangwonApplicationResponse> getAdminApplications(
            String email,
            int page,
            int size,
            String status,
            String keyword
    ) {
        validateAdmin(email);

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(
                        Sort.Order.desc("appliedAt"),
                        Sort.Order.desc("jangwonApplicationId")
                )
        );

        JangwonStatus jangwonStatus = parseStatus(status);
        String searchKeyword = hasText(keyword) ? "%" + keyword.trim() + "%" : null;

        Page<JangwonApplication> applicationPage =
                jangwonApplicationRepository.searchForAdmin(
                        jangwonStatus,
                        searchKeyword,
                        pageRequest
                );

        return toApplicationPageResponse(applicationPage);
    }

    public JangwonApplicationResponse getAdminApplicationDetail(
            String email,
            Long jangwonApplicationId
    ) {
        validateAdmin(email);

        JangwonApplication application = getApplication(jangwonApplicationId);

        return JangwonApplicationResponse.from(application);
    }

    @Transactional
    public JangwonApplicationResponse approve(
            String email,
            Long jangwonApplicationId
    ) {
        validateAdmin(email);

        JangwonApplication application = getApplication(jangwonApplicationId);

        if (application.getStatus() == JangwonStatus.APPROVED) {
            throw new BusinessException(
                    "JANGWON_ALREADY_APPROVED",
                    "이미 승인된 신청입니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        application.approve();

        return JangwonApplicationResponse.from(application);
    }

    @Transactional
    public JangwonApplicationResponse reject(
            String email,
            Long jangwonApplicationId,
            JangwonRejectRequest request
    ) {
        validateAdmin(email);

        JangwonApplication application = getApplication(jangwonApplicationId);

        if (application.getStatus() == JangwonStatus.APPROVED) {
            throw new BusinessException(
                    "JANGWON_ALREADY_APPROVED",
                    "이미 승인된 신청은 반려할 수 없습니다",
                    HttpStatus.BAD_REQUEST
            );
        }

        application.reject(request.adminMemo().trim());

        return JangwonApplicationResponse.from(application);
    }

    @Transactional
    public void delete(
            String email,
            Long jangwonApplicationId
    ) {
        validateAdmin(email);

        JangwonApplication application = getApplication(jangwonApplicationId);

        jangwonApplicationRepository.delete(application);
    }

    private JangwonApplication getApplication(Long jangwonApplicationId) {
        return jangwonApplicationRepository.findById(jangwonApplicationId)
                .orElseThrow(() -> new BusinessException(
                        "JANGWON_NOT_FOUND",
                        "장원급제 신청을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private PageResponse<JangwonApplicationResponse> toApplicationPageResponse(
            Page<JangwonApplication> applicationPage
    ) {
        return new PageResponse<>(
                applicationPage.getContent()
                        .stream()
                        .map(JangwonApplicationResponse::from)
                        .toList(),
                applicationPage.getTotalElements(),
                applicationPage.getTotalPages(),
                applicationPage.getNumber(),
                applicationPage.getSize()
        );
    }

    private Member getCurrentMember(String email) {
        if (!hasText(email)) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return memberRepository.findByEmail(email)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "회원 정보를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private void validateAdmin(String email) {
        Member member = getCurrentMember(email);

        if (member.getUserRole() != MemberRole.ADMIN) {
            throw new BusinessException(
                    "ADMIN_FORBIDDEN",
                    "관리자만 접근할 수 있습니다",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private JangwonStatus parseStatus(String status) {
        if (!hasText(status)) {
            return null;
        }

        try {
            return JangwonStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "INVALID_JANGWON_STATUS",
                    "장원급제 상태값이 올바르지 않습니다",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
