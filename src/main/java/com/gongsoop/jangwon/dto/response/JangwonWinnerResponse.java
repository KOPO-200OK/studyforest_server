package com.gongsoop.jangwon.dto.response;

import com.gongsoop.jangwon.entity.JangwonApplication;

import java.time.LocalDateTime;

public record JangwonWinnerResponse(
        Long jangwonApplicationId,
        Long memberId,
        String displayNickname,
        String displayName,
        String characterName,
        String characterImageUrl,
        LocalDateTime reviewedAt
) {

    public static JangwonWinnerResponse from(JangwonApplication application) {
        return new JangwonWinnerResponse(
                application.getJangwonApplicationId(),
                application.getMemberId(),
                application.getDisplayNickname(),
                application.getDisplayName(),
                application.getCharacterName(),
                application.getCharacterImageUrl(),
                application.getReviewedAt()
        );
    }
}