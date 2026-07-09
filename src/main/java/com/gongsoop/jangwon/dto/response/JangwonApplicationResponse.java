package com.gongsoop.jangwon.dto.response;

import com.gongsoop.jangwon.entity.JangwonApplication;

import java.time.LocalDateTime;

public record JangwonApplicationResponse(
        Long jangwonApplicationId,
        Long memberId,
        String displayNickname,
        String displayName,
        String characterName,
        String characterImageUrl,
        String certificateImageUrl,
        String status,
        String adminMemo,
        Boolean isVisible,
        LocalDateTime appliedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static JangwonApplicationResponse from(JangwonApplication application) {
        return new JangwonApplicationResponse(
                application.getJangwonApplicationId(),
                application.getMemberId(),
                application.getDisplayNickname(),
                application.getDisplayName(),
                application.getCharacterName(),
                application.getCharacterImageUrl(),
                application.getCertificateImageUrl(),
                application.getStatus().name(),
                application.getAdminMemo(),
                application.isVisible(),
                application.getAppliedAt(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}